package com.pxwork.api.controller.course;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pxwork.common.utils.Result;
import com.pxwork.course.entity.CourseChapter;
import com.pxwork.course.service.CourseChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "课程章节管理")
@RestController
@RequestMapping("/course/chapter")
public class CourseChapterController {

    @Autowired
    private CourseChapterService courseChapterService;

    @Operation(summary = "获取课程章节列表")
    @GetMapping("/list")
    public Result<List<CourseChapter>> list(@RequestParam Long courseId) {
        LambdaQueryWrapper<CourseChapter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseChapter::getCourseId, courseId);
        queryWrapper.orderByAsc(CourseChapter::getSort);
        return Result.success(courseChapterService.list(queryWrapper));
    }

    @Operation(summary = "创建章节")
    @PostMapping
    public Result<Boolean> create(@RequestBody CourseChapter chapter) {
        return Result.success(courseChapterService.save(chapter));
    }

    @Operation(summary = "更新章节")
    @PutMapping
    public Result<Boolean> update(@RequestBody CourseChapter chapter) {
        return Result.success(courseChapterService.updateById(chapter));
    }

    @Operation(summary = "删除章节(级联删除课时)")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(courseChapterService.removeChapterWithHours(id));
    }
}
