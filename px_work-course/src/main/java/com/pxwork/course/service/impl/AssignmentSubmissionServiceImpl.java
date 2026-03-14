package com.pxwork.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.AssignmentSubmission;
import com.pxwork.course.mapper.AssignmentSubmissionMapper;
import com.pxwork.course.service.AssignmentSubmissionService;
import org.springframework.stereotype.Service;

@Service
public class AssignmentSubmissionServiceImpl extends ServiceImpl<AssignmentSubmissionMapper, AssignmentSubmission> implements AssignmentSubmissionService {
}
