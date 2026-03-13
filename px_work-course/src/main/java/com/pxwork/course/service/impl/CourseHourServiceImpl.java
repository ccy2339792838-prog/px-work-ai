package com.pxwork.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.CourseHour;
import com.pxwork.course.mapper.CourseHourMapper;
import com.pxwork.course.service.CourseHourService;
import org.springframework.stereotype.Service;

@Service
public class CourseHourServiceImpl extends ServiceImpl<CourseHourMapper, CourseHour> implements CourseHourService {
}
