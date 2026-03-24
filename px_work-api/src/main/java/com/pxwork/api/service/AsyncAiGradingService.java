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

                // 呼叫 AI
                String aiOutputJson = difyApiService.runGradeWorkflow(inputs);
                
                // 解析 AI 结果
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

    /**
     * 核心修复：防弹级大模型JSON解析器
     */
    private AiJudgeResult parseAiJudgeResult(String aiOutputJson) {
        BigDecimal score = BigDecimal.ZERO;
        String comment = "系统解析AI评分失败，需人工复核"; // 兜底评语
        try {
            log.info("====== 开始解析 AI 判卷结果 ======");
            log.info("Dify 原始返回包: {}", aiOutputJson);

            String cleaned = JsonUtils.cleanMarkdownJson(aiOutputJson);
            if (!StringUtils.hasText(cleaned)) {
                return new AiJudgeResult(score, comment);
            }

            // 1. 转为 JSON 对象
            Object parsed = JSONUtil.parse(cleaned);
            JSONObject rootObj;
            if (parsed instanceof JSONObject) {
                rootObj = (JSONObject) parsed;
            } else if (parsed instanceof JSONArray && !((JSONArray) parsed).isEmpty()) {
                rootObj = JSONUtil.parseObj(((JSONArray) parsed).get(0));
            } else {
                return new AiJudgeResult(score, comment);
            }

            JSONObject targetObj = rootObj;

            // 2. 剥洋葱机制：专门应对 Dify 返回嵌套文本 JSON 的情况
            if (!hasScoreKey(rootObj)) {
                boolean foundNested = false;
                // 遍历寻找内部是不是包了一层 String 类型的 JSON
                for (String key : rootObj.keySet()) {
                    Object val = rootObj.get(key);
                    if (val instanceof String) {
                        String strVal = ((String) val).trim();
                        strVal = strVal.replaceAll("(?i)```json", "").replaceAll("```", "").trim();
                        if (strVal.startsWith("{") && (strVal.contains("\"score\"") || strVal.contains("\"ai_score\""))) {
                            try {
                                targetObj = JSONUtil.parseObj(strVal);
                                log.info("成功从外层字段 [{}] 中剥离出真实的 AI 答卷 JSON!", key);
                                foundNested = true;
                                break;
                            } catch (Exception ignored) {}
                        }
                    } else if (val instanceof JSONObject) {
                        // 如果有更深层，比如 {"data": {"text": "{\"score\": 10}"}}
                        JSONObject nestedObj = (JSONObject) val;
                        for (String innerKey : nestedObj.keySet()) {
                            Object innerVal = nestedObj.get(innerKey);
                            if (innerVal instanceof String) {
                                String innerStr = ((String) innerVal).trim();
                                innerStr = innerStr.replaceAll("(?i)```json", "").replaceAll("```", "").trim();
                                if (innerStr.startsWith("{") && (innerStr.contains("\"score\"") || innerStr.contains("\"ai_score\""))) {
                                    try {
                                        targetObj = JSONUtil.parseObj(innerStr);
                                        log.info("成功从嵌套字段 [{}] 中剥离出真实的 AI 答卷 JSON!", innerKey);
                                        foundNested = true;
                                        break;
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                    if (foundNested) break;
                }
            }

            // 3. 提取分数和评语
            score = extractScore(targetObj);
            comment = extractComment(targetObj);
            
            log.info("====== 解析成功！最终提取分数: {}, 评语: {} ======", score, comment);
            return new AiJudgeResult(score, comment);

        } catch (Exception e) {
            log.error("❌ 解析 AI 结果时发生严重异常！原始包内容: {}", aiOutputJson, e);
            return new AiJudgeResult(BigDecimal.ZERO, "系统解析AI评分失败，需人工复核");
        }
    }

    private boolean hasScoreKey(JSONObject jsonObject) {
        return jsonObject.containsKey("score") || jsonObject.containsKey("ai_score") || jsonObject.containsKey("final_score");
    }

    private BigDecimal extractScore(JSONObject jsonObject) {
        Object scoreValue = jsonObject.get("score");
        if (scoreValue == null) scoreValue = jsonObject.get("ai_score");
        if (scoreValue == null) scoreValue = jsonObject.get("final_score");
        
        if (scoreValue == null) return BigDecimal.ZERO;
        
        try {
            return new BigDecimal(String.valueOf(scoreValue));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String extractComment(JSONObject jsonObject) {
        // 饱和式提取，防止 AI 乱换名字
        String[] possibleKeys = {"comment", "ai_comment", "aiComment", "feedback", "reason"};
        for (String key : possibleKeys) {
            String val = jsonObject.getStr(key);
            if (StringUtils.hasText(val)) {
                return val;
            }
        }
        return "";
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