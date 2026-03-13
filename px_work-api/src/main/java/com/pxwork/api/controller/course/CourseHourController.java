package com.pxwork.api.controller.course;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pxwork.common.utils.Result;
import com.pxwork.course.entity.CourseHour;
import com.pxwork.course.service.CourseHourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "课程课时管理")
@RestController
@RequestMapping("/course/hour")
public class CourseHourController {

    @Autowired
    private CourseHourService courseHourService;

    @Operation(summary = "获取章节课时列表")
    @GetMapping("/list")
    public Result<List<CourseHour>> list(@RequestParam Long chapterId) {
        LambdaQueryWrapper<CourseHour> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseHour::getChapterId, chapterId);
        queryWrapper.orderByAsc(CourseHour::getSort);
        return Result.success(courseHourService.list(queryWrapper));
    }

    @Operation(summary = "创建课时")
    @PostMapping
    public Result<Boolean> create(@RequestBody CourseHour hour) {
        return Result.success(courseHourService.save(hour));
    }

    @Operation(summary = "更新课时")
    @PutMapping
    public Result<Boolean> update(@RequestBody CourseHour hour) {
        return Result.success(courseHourService.updateById(hour));
    }

    @Operation(summary = "删除课时")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(courseHourService.removeById(id));
    }

    @Operation(summary = "获取课时详情")
    @GetMapping("/{id}")
    public Result<CourseHour> get(@PathVariable Long id) {
        return Result.success(courseHourService.getById(id));
    }
}
