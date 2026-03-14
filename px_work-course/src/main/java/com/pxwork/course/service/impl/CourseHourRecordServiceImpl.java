package com.pxwork.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.CourseHourRecord;
import com.pxwork.course.mapper.CourseHourRecordMapper;
import com.pxwork.course.service.CourseHourRecordService;
import org.springframework.stereotype.Service;

@Service
public class CourseHourRecordServiceImpl extends ServiceImpl<CourseHourRecordMapper, CourseHourRecord> implements CourseHourRecordService {
}
