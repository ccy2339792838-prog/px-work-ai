package com.pxwork.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.CourseAssignment;
import com.pxwork.course.mapper.CourseAssignmentMapper;
import com.pxwork.course.service.CourseAssignmentService;
import org.springframework.stereotype.Service;

@Service
public class CourseAssignmentServiceImpl extends ServiceImpl<CourseAssignmentMapper, CourseAssignment> implements CourseAssignmentService {
}
