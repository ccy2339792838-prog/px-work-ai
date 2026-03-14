package com.pxwork.api.controller.frontend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pxwork.common.utils.Result;
import com.pxwork.common.utils.StpUserUtil;
import com.pxwork.course.entity.CourseChapter;
import com.pxwork.course.entity.CourseHour;
import com.pxwork.course.entity.CourseHourRecord;
import com.pxwork.course.service.CourseChapterService;
import com.pxwork.course.service.CourseHourRecordService;
import com.pxwork.course.service.CourseHourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "前台学习进度")
@RestController
@RequestMapping("/frontend/progress")
public class FrontendProgressController {

    @Autowired
    private CourseHourRecordService courseHourRecordService;
    @Autowired
    private CourseChapterService courseChapterService;
    @Autowired
    private CourseHourService courseHourService;

    @Operation(summary = "上报学习进度")
    @PostMapping("/report")
    public Result<Map<String, Object>> report(@RequestBody @Validated ProgressReportRequest request) {
        long userId = StpUserUtil.getLoginIdAsLong();
        long resourceId = request.getResourceId() == null ? request.getHourId() : request.getResourceId();
        if (resourceId <= 0) {
            return Result.fail("资源ID无效");
        }
        int totalDuration = request.getTotalDuration();
        int currentTime = request.getCurrentTime();
        if (totalDuration <= 0) {
            return Result.fail("总时长必须大于0");
        }

        CourseHourRecord record = courseHourRecordService.getOne(new LambdaQueryWrapper<CourseHourRecord>()
                .eq(CourseHourRecord::getUserId, userId)
                .eq(CourseHourRecord::getCourseId, request.getCourseId())
                .eq(CourseHourRecord::getResourceId, resourceId));

        int finishedDuration;
        if (record == null) {
            record = new CourseHourRecord();
            record.setUserId(userId);
            record.setCourseId(request.getCourseId());
            record.setResourceId(resourceId);
            record.setTotalDuration(totalDuration);
            finishedDuration = Math.max(0, currentTime);
            record.setFinishedDuration(finishedDuration);
        } else {
            record.setTotalDuration(Math.max(record.getTotalDuration() == null ? 0 : record.getTotalDuration(), totalDuration));
            finishedDuration = Math.max(record.getFinishedDuration() == null ? 0 : record.getFinishedDuration(), currentTime);
            record.setFinishedDuration(finishedDuration);
        }
        int recordTotal = record.getTotalDuration() == null ? totalDuration : record.getTotalDuration();
        boolean isFinished = finishedDuration * 100L >= recordTotal * 95L;
        record.setIsFinished(isFinished ? 1 : 0);

        if (record.getId() == null) {
            courseHourRecordService.save(record);
        } else {
            courseHourRecordService.updateById(record);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("finishedDuration", record.getFinishedDuration());
        result.put("isFinished", record.getIsFinished() == 1);
        return Result.success(result);
    }

    @Operation(summary = "校验课程完成度")
    @GetMapping("/check-completion/{courseId}")
    public Result<Map<String, Object>> checkCompletion(@PathVariable Long courseId) {
        long userId = StpUserUtil.getLoginIdAsLong();
        List<Long> chapterIds = courseChapterService.list(new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourseId, courseId))
                .stream()
                .map(CourseChapter::getId)
                .collect(Collectors.toList());

        long totalHours = 0;
        if (!chapterIds.isEmpty()) {
            totalHours = courseHourService.count(new LambdaQueryWrapper<CourseHour>()
                    .in(CourseHour::getChapterId, chapterIds));
        }
        long finishedHours = courseHourRecordService.count(new LambdaQueryWrapper<CourseHourRecord>()
                .eq(CourseHourRecord::getUserId, userId)
                .eq(CourseHourRecord::getCourseId, courseId)
                .eq(CourseHourRecord::getIsFinished, 1));

        Map<String, Object> result = new HashMap<>();
        result.put("isCompleted", totalHours > 0 && finishedHours == totalHours);
        result.put("totalHours", totalHours);
        result.put("finishedHours", finishedHours);
        return Result.success(result);
    }

    @Data
    public static class ProgressReportRequest {
        @NotNull(message = "课程ID不能为空")
        private Long courseId;
        @NotNull(message = "课时ID不能为空")
        private Long hourId;
        @NotNull(message = "资源ID不能为空")
        private Long resourceId;
        @NotNull(message = "总时长不能为空")
        @Min(value = 1, message = "总时长必须大于0")
        private Integer totalDuration;
        @NotNull(message = "当前进度不能为空")
        @Min(value = 0, message = "当前进度不能小于0")
        private Integer currentTime;
    }
}
