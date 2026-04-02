package com.pxwork.api.controller.frontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pxwork.common.utils.Result;
import com.pxwork.common.utils.StpUserUtil;
import com.pxwork.course.entity.Course;
import com.pxwork.course.entity.UserCourseEnrollment;
import com.pxwork.course.service.CourseService;
import com.pxwork.course.service.UserCourseEnrollmentService;

import com.pxwork.course.entity.CourseChapter;
import com.pxwork.course.entity.CourseHour;
import com.pxwork.course.entity.CourseResource;
import com.pxwork.course.entity.Exam;
import com.pxwork.course.service.CourseChapterService;
import com.pxwork.course.service.CourseHourService;
import com.pxwork.course.service.CourseResourceService;
import com.pxwork.course.service.ExamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

@Tag(name = "4.2 前台-选课与课程大厅")
@RestController
@RequestMapping("/frontend/course")
public class FrontendCourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserCourseEnrollmentService userCourseEnrollmentService;

    @Autowired
    private CourseChapterService courseChapterService;
    
    @Autowired
    private CourseHourService courseHourService;
    
    @Autowired
    private CourseResourceService courseResourceService;
    
    @Autowired
    private ExamService examService;

    @Operation(summary = "获取已发布课程列表", description = "获取所有已发布且对学员可见的课程列表")
    @GetMapping("/list")
    public Result<List<Course>> list() {
        return Result.success(courseService.getPublishedCourses());
    }
    
    @Operation(summary = "获取课程详情(含大纲/课时/资源/考试大礼包)")
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Course course = courseService.getCourseDetails(id);
        if (course == null) {
            return Result.fail("课程不存在");
        }
        if (course.getStatus() != null && course.getStatus() == 0) {
            return Result.fail("课程未发布或已下架");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("course", course);

        // 3. 查出该课程的所有【章节】
        List<CourseChapter> chapters = courseChapterService.list(
                new LambdaQueryWrapper<CourseChapter>().eq(CourseChapter::getCourseId, id));
        result.put("chapters", chapters); // 保留平铺数据，防呆

        // 4. 🔴 终极树形结构逻辑：组装 chapterTree
        List<CourseHour> hours = new ArrayList<>();
        List<Map<String, Object>> chapterTree = new ArrayList<>(); // 用来装“章节+课时”的树
        
        if (!chapters.isEmpty()) {
            List<Long> chapterIds = chapters.stream().map(CourseChapter::getId).collect(Collectors.toList());
            
            // 查出所有课时，并按照 sort(排序) 升序排列！保证 1-1 在 1-2 前面
            hours = courseHourService.list(
                    new LambdaQueryWrapper<CourseHour>()
                            .in(CourseHour::getChapterId, chapterIds)
                            .orderByAsc(CourseHour::getSort));

            // 将课时按照 chapterId 分组
            Map<Long, List<CourseHour>> hourMap = hours.stream().collect(Collectors.groupingBy(CourseHour::getChapterId));

            // 拼装树！
            for (CourseChapter chapter : chapters) {
                Map<String, Object> node = new java.util.LinkedHashMap<>();
                node.put("chapter", chapter); // 放入章节实体
                node.put("hours", hourMap.getOrDefault(chapter.getId(), new ArrayList<>())); // 放入该章节下的课时列表
                chapterTree.add(node);
            }
        }
        result.put("hours", hours); // 保留平铺数据，防呆
        result.put("chapterTree", chapterTree); // 🔴 送给前端的完美树形结构

        // 5. 查出该课程的所有【课件资源】
        List<CourseResource> resources = courseResourceService.list(
                new LambdaQueryWrapper<CourseResource>().eq(CourseResource::getCourseId, id));
        result.put("resources", resources);

        // 6. 查出该课程关联的所有【考试】
        List<Exam> exams = examService.list(
                new LambdaQueryWrapper<Exam>().eq(Exam::getCourseId, id));
        result.put("exams", exams);

        return Result.success(result);
    }

    @Operation(summary = "选课")
    @PostMapping("/enroll/{courseId}")
    public Result<Boolean> enroll(@PathVariable Long courseId) {
        long userId = StpUserUtil.getLoginIdAsLong();
        Course course = courseService.getById(courseId);
        if (course == null) {
            return Result.fail("课程不存在");
        }
        long exists = userCourseEnrollmentService.count(new LambdaQueryWrapper<UserCourseEnrollment>()
                .eq(UserCourseEnrollment::getUserId, userId)
                .eq(UserCourseEnrollment::getCourseId, courseId));
        if (exists > 0) {
            return Result.fail("已选过该课程");
        }
        UserCourseEnrollment enrollment = new UserCourseEnrollment();
        enrollment.setUserId(userId);
        enrollment.setCourseId(courseId);
        enrollment.setStatus(0);
        return Result.success(userCourseEnrollmentService.save(enrollment));
    }

    @Operation(summary = "我的课程")
    @GetMapping("/my-courses")
    public Result<List<MyCourseVO>> myCourses() {
        long userId = StpUserUtil.getLoginIdAsLong();
        List<UserCourseEnrollment> enrollments = userCourseEnrollmentService.list(
                new LambdaQueryWrapper<UserCourseEnrollment>()
                        .eq(UserCourseEnrollment::getUserId, userId)
                        .orderByDesc(UserCourseEnrollment::getCreatedAt));
        if (enrollments.isEmpty()) {
            return Result.success(List.of());
        }
        Set<Long> courseIds = enrollments.stream().map(UserCourseEnrollment::getCourseId).collect(Collectors.toSet());
        List<Course> courses = courseService.list(new LambdaQueryWrapper<Course>().in(Course::getId, courseIds));
        Map<Long, Course> courseMap = new HashMap<>();
        for (Course course : courses) {
            courseMap.put(course.getId(), course);
        }
        List<MyCourseVO> result = enrollments.stream()
                .map(enrollment -> {
                    Course course = courseMap.get(enrollment.getCourseId());
                    if (course == null) {
                        return null;
                    }
                    MyCourseVO vo = new MyCourseVO();
                    vo.setCourseId(course.getId());
                    vo.setName(course.getName());
                    vo.setTitle(course.getTitle());
                    vo.setThumb(course.getThumb());
                    vo.setShortDesc(course.getShortDesc());
                    vo.setCreditHours(course.getCreditHours());
                    vo.setCourseStatus(course.getStatus());
                    vo.setLearningStatus(enrollment.getStatus());
                    return vo;
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @Data
    public static class MyCourseVO {
        private Long courseId;
        private String name;
        private String title;
        private String thumb;
        private String shortDesc;
        private java.math.BigDecimal creditHours;
        private Integer courseStatus;
        private Integer learningStatus;
    }
}