package com.pxwork.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.ProcessEvaluation;
import com.pxwork.course.mapper.ProcessEvaluationMapper;
import com.pxwork.course.service.ProcessEvaluationService;
import org.springframework.stereotype.Service;

@Service
public class ProcessEvaluationServiceImpl extends ServiceImpl<ProcessEvaluationMapper, ProcessEvaluation> implements ProcessEvaluationService {
}
