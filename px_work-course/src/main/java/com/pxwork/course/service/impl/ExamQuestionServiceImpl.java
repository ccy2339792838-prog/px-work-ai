package com.pxwork.course.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.Exam;
import com.pxwork.course.entity.ExamQuestion;
import com.pxwork.course.entity.Question;
import com.pxwork.course.mapper.ExamQuestionMapper;
import com.pxwork.course.service.ExamQuestionService;
import com.pxwork.course.service.ExamService;
import com.pxwork.course.service.QuestionService;

@Service
public class ExamQuestionServiceImpl extends ServiceImpl<ExamQuestionMapper, ExamQuestion> implements ExamQuestionService {

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionService questionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> bindQuestions(Long examId, List<Long> questionIds) {
        Exam exam = examService.getById(examId);
        if (exam == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        if (questionIds == null || questionIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要绑定的题目");
        }
        List<Long> distinctRequestedIds = questionIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
        if (distinctRequestedIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要绑定的题目");
        }

        List<Question> existingQuestions = questionService.list(new LambdaQueryWrapper<Question>()
                .in(Question::getId, distinctRequestedIds));
        if (existingQuestions.isEmpty()) {
            throw new IllegalArgumentException("所选题目不存在");
        }
        Set<Long> validQuestionIds = existingQuestions.stream()
                .map(Question::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ExamQuestion> boundRelations = this.list(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .in(ExamQuestion::getQuestionId, validQuestionIds));
        Set<Long> alreadyBoundIds = boundRelations.stream()
                .map(ExamQuestion::getQuestionId)
                .collect(Collectors.toSet());

        List<Long> toBindIds = validQuestionIds.stream()
                .filter(id -> !alreadyBoundIds.contains(id))
                .collect(Collectors.toList());

        ExamQuestion maxSortRelation = this.getOne(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .orderByDesc(ExamQuestion::getSort)
                .last("limit 1"));
        int nextSort = maxSortRelation == null || maxSortRelation.getSort() == null ? 1 : maxSortRelation.getSort() + 1;

        List<ExamQuestion> toSave = new ArrayList<>();
        for (Long questionId : toBindIds) {
            ExamQuestion relation = new ExamQuestion();
            relation.setExamId(examId);
            relation.setQuestionId(questionId);
            relation.setScore(BigDecimal.ONE);
            relation.setSort(nextSort++);
            toSave.add(relation);
        }

        if (!toSave.isEmpty()) {
            this.saveBatch(toSave);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("requestedCount", distinctRequestedIds.size());
        result.put("validCount", validQuestionIds.size());
        result.put("alreadyBoundCount", alreadyBoundIds.size());
        result.put("addedCount", toSave.size());
        result.put("addedQuestionIds", toBindIds);
        return result;
    }
}
