package com.pxwork.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Tag(name = "课程管理", description = "课程相关的接口")
@RestController
@RequestMapping("/course")
public class CourseController {

    @Operation(summary = "课程列表", description = "获取所有课程信息")
    @GetMapping("/list")
    public List<String> list() {
        return Collections.singletonList("Course 1");
    }

    @Operation(summary = "课程详情", description = "获取单个课程的详细信息")
    @GetMapping("/detail")
    public String detail() {
        return "Course Detail";
    }
}
