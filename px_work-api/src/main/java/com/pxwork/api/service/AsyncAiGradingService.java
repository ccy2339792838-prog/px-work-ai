package com.pxwork.api.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pxwork.common.service.ai.DifyApiService;
import com.pxwork.common.utils.JsonUtils;
import com.pxwork.course.entity.Exam;
import com.pxwork.course.entity.Question;
import com.pxwork.course.entity.UserExam;
import com.pxwork.course.entity.UserExamAnswer;
import com.pxwork.course.service.ExamService;
import com.pxwork.course.service.QuestionService;
import com.pxwork.course.service.UserExamAnswerService;
import com.pxwork.course.service.UserExamService;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AsyncAiGradingService {

    @Autowired
    private UserExamAnswerService userExamAnswerService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserExamService userExamService;

    @Autowired
    private ExamService examService;

    @Autowired
    private DifyApiService difyApiService;

    @Async
    @Transactional(rollbackFor = Exception.class)
    public void gradeSubjectiveAnswers(Long userExamId) {
        try {
            List<UserExamAnswer> subjectiveAnswers = userExamAnswerService.list(new LambdaQueryWrapper<UserExamAnswer>()
                    .eq(UserExamAnswer::getUserExamId, userExamId)
                    .isNull(UserExamAnswer::getIsCorrect));
            if (subjectiveAnswers.isEmpty()) {
                return;
            }

            Set<Long> questionIds = subjectiveAnswers.stream()
                    .map(UserExamAnswer::getQuestionId)
                    .collect(Collectors.toSet());
            List<Question> questionList = questionService.list(new LambdaQueryWrapper<Question>().in(Question::getId, questionIds));
            Map<Long, Question> questionMap = questionList.stream()
                    .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

            List<UserExamAnswer> updates = new ArrayList<>();
            for (UserExamAnswer answer : subjectiveAnswers) {
                Question question = questionMap.get(answer.getQuestionId());
                if (question == null) {
                    continue;
                }
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("question", question.getContent() == null ? "" : question.getContent());
                inputs.put("standard_answer", question.getStandardAnswer() == null ? "" : question.getStandardAnswer());
                inputs.put("student_answer", answer.getUserAnswer() == null ? "" : answer.getUserAnswer().trim());

                String aiOutputJson = difyApiService.runGradeWorkflow(inputs);
                AiJudgeResult aiJudgeResult = parseAiJudgeResult(aiOutputJson);
                answer.setScore(aiJudgeResult.getScore());
                answer.setAiComment(aiJudgeResult.getComment());
                updates.add(answer);
            }
            if (!updates.isEmpty()) {
                userExamAnswerService.updateBatchById(updates);
            }

            List<UserExamAnswer> refreshedSubjectiveAnswers = userExamAnswerService.list(new LambdaQueryWrapper<UserExamAnswer>()
                    .eq(UserExamAnswer::getUserExamId, userExamId)
                    .isNull(UserExamAnswer::getIsCorrect));
            BigDecimal subjectiveScore = refreshedSubjectiveAnswers.stream()
                    .map(UserExamAnswer::getScore)
                    .filter(item -> item != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            UserExam userExam = userExamService.getById(userExamId);
            if (userExam == null) {
                return;
            }
            userExam.setSubjectiveScore(subjectiveScore);
            userExam.setStatus(2);
            userExamService.updateById(userExam);

            Exam exam = examService.getById(userExam.getExamId());
            BigDecimal practicalWeight = exam == null || exam.getWeightPractical() == null ? BigDecimal.ZERO : exam.getWeightPractical();
            if (practicalWeight.compareTo(BigDecimal.ZERO) == 0) {
                userExamService.calculateFinalResult(userExamId);
            }
        } catch (Exception e) {
            log.error("Async AI grading failed, userExamId={}", userExamId, e);
        }
    }

    private AiJudgeResult parseAiJudgeResult(String aiOutputJson) {
        BigDecimal score = BigDecimal.ZERO;
        String comment = "";
        String cleaned = JsonUtils.cleanMarkdownJson(aiOutputJson);
        if (!StringUtils.hasText(cleaned)) {
            return new AiJudgeResult(score, comment);
        }
        Object parsed = JSONUtil.parse(cleaned);
        JSONObject source;
        if (parsed instanceof JSONObject jsonObject) {
            source = jsonObject;
        } else if (parsed instanceof JSONArray jsonArray && !jsonArray.isEmpty()) {
            source = JSONUtil.parseObj(jsonArray.get(0));
        } else {
            return new AiJudgeResult(score, comment);
        }
        JSONObject candidate = source;
        Object nested = source.get("result");
        if (!(nested instanceof JSONObject)) {
            nested = source.get("data");
        }
        if (!(nested instanceof JSONObject)) {
            nested = source.get("output");
        }
        if (nested instanceof JSONObject nestedObj) {
            candidate = nestedObj;
        }
        score = extractScore(candidate);
        comment = extractComment(candidate);
        return new AiJudgeResult(score, comment);
    }

    private BigDecimal extractScore(JSONObject jsonObject) {
        Object scoreValue = jsonObject.get("score");
        if (scoreValue == null) {
            scoreValue = jsonObject.get("ai_score");
        }
        if (scoreValue == null) {
            scoreValue = jsonObject.get("final_score");
        }
        if (scoreValue == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(scoreValue));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String extractComment(JSONObject jsonObject) {
        String comment = jsonObject.getStr("comment");
        if (StringUtils.hasText(comment)) {
            return comment;
        }
        comment = jsonObject.getStr("ai_comment");
        if (StringUtils.hasText(comment)) {
            return comment;
        }
        comment = jsonObject.getStr("feedback");
        if (StringUtils.hasText(comment)) {
            return comment;
        }
        comment = jsonObject.getStr("reason");
        return comment == null ? "" : comment;
    }

    private static class AiJudgeResult {
        private final BigDecimal score;
        private final String comment;

        private AiJudgeResult(BigDecimal score, String comment) {
            this.score = score;
            this.comment = comment;
        }

        public BigDecimal getScore() {
            return score;
        }

        public String getComment() {
            return comment;
        }
    }
}
