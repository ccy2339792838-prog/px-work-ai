package com.pxwork.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.Course;
import com.pxwork.course.entity.CourseChapter;
import com.pxwork.course.entity.CourseHour;
import com.pxwork.course.mapper.CourseMapper;
import com.pxwork.course.service.CourseChapterService;
import com.pxwork.course.service.CourseHourService;
import com.pxwork.course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    @Autowired
    private CourseChapterService courseChapterService;

    @Autowired
    private CourseHourService courseHourService;

    @Override
    public Course getCourseDetails(Long courseId) {
        Course course = getById(courseId);
        if (course == null) {
            return null;
        }

        // 获取章节
        LambdaQueryWrapper<CourseChapter> chapterQueryWrapper = new LambdaQueryWrapper<>();
        chapterQueryWrapper.eq(CourseChapter::getCourseId, courseId);
        chapterQueryWrapper.orderByAsc(CourseChapter::getSort);
        List<CourseChapter> chapters = courseChapterService.list(chapterQueryWrapper);

        if (chapters != null && !chapters.isEmpty()) {
            List<Long> chapterIds = chapters.stream().map(CourseChapter::getId).collect(Collectors.toList());
            
            // 获取所有相关课时
            LambdaQueryWrapper<CourseHour> hourQueryWrapper = new LambdaQueryWrapper<>();
            hourQueryWrapper.in(CourseHour::getChapterId, chapterIds);
            hourQueryWrapper.orderByAsc(CourseHour::getSort);
            List<CourseHour> allHours = courseHourService.list(hourQueryWrapper);

            // 分组
            Map<Long, List<CourseHour>> hoursMap = allHours.stream()
                    .collect(Collectors.groupingBy(CourseHour::getChapterId));

            // 组装
            for (CourseChapter chapter : chapters) {
                chapter.setHours(hoursMap.getOrDefault(chapter.getId(), new ArrayList<>()));
            }
        }

        course.setChapters(chapters);
        return course;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeCourseWithRelations(Long courseId) {
        // 获取所有章节
        LambdaQueryWrapper<CourseChapter> chapterQueryWrapper = new LambdaQueryWrapper<>();
        chapterQueryWrapper.eq(CourseChapter::getCourseId, courseId);
        List<CourseChapter> chapters = courseChapterService.list(chapterQueryWrapper);

        if (chapters != null && !chapters.isEmpty()) {
            for (CourseChapter chapter : chapters) {
                // 调用级联删除章节的方法
                courseChapterService.removeChapterWithHours(chapter.getId());
            }
        }

        // 删除课程本身
        return removeById(courseId);
    }
}
