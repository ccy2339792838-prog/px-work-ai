package com.pxwork.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pxwork.course.entity.CourseChapter;

import java.io.Serializable;

public interface CourseChapterService extends IService<CourseChapter> {
    boolean removeChapterWithHours(Long chapterId);
}
