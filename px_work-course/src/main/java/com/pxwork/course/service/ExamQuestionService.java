package com.pxwork.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pxwork.course.entity.ExamQuestion;

import java.util.List;
import java.util.Map;

public interface ExamQuestionService extends IService<ExamQuestion> {
    Map<String, Object> bindQuestions(Long examId, List<Long> questionIds);
}
