package com.pxwork.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pxwork.course.entity.CourseCategory;

import java.util.List;

public interface CourseCategoryService extends IService<CourseCategory> {
    List<CourseCategory> listTree();
}
