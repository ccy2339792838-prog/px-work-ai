package com.pxwork.course.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pxwork.course.entity.CourseCategory;

public interface CourseCategoryService extends IService<CourseCategory> {
    List<CourseCategory> listTree(String industry);
}
