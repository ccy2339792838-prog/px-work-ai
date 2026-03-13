package com.pxwork.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.CourseChapter;
import com.pxwork.course.entity.CourseHour;
import com.pxwork.course.mapper.CourseChapterMapper;
import com.pxwork.course.service.CourseChapterService;
import com.pxwork.course.service.CourseHourService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseChapterServiceImpl extends ServiceImpl<CourseChapterMapper, CourseChapter> implements CourseChapterService {

    @Autowired
    private CourseHourService courseHourService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeChapterWithHours(Long chapterId) {
        // 删除该章节下的所有课时
        LambdaQueryWrapper<CourseHour> hourQueryWrapper = new LambdaQueryWrapper<>();
        hourQueryWrapper.eq(CourseHour::getChapterId, chapterId);
        courseHourService.remove(hourQueryWrapper);
        
        // 删除章节本身
        return removeById(chapterId);
    }
}
