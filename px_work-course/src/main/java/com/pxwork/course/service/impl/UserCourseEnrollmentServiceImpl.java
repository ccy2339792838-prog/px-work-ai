package com.pxwork.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.UserCourseEnrollment;
import com.pxwork.course.mapper.UserCourseEnrollmentMapper;
import com.pxwork.course.service.UserCourseEnrollmentService;
import org.springframework.stereotype.Service;

@Service
public class UserCourseEnrollmentServiceImpl extends ServiceImpl<UserCourseEnrollmentMapper, UserCourseEnrollment> implements UserCourseEnrollmentService {
}
