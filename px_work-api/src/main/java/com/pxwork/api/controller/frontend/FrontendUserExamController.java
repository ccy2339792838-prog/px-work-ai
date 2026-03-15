package com.pxwork.api.controller.frontend;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pxwork.api.service.AsyncAiGradingService;
import com.pxwork.common.utils.Result;
import com.pxwork.common.utils.StpUserUtil;
import com.pxwork.course.entity.Exam;
import com.pxwork.course.entity.ExamQuestion;
import com.pxwork.course.entity.Question;
import com.pxwork.course.entity.UserCourseEnrollment;
import com.pxwork.course.entity.UserExam;
import com.pxwork.course.entity.UserExamAnswer;
import com.pxwork.course.service.ExamQuestionService;
import com.pxwork.course.service.ExamService;
import com.pxwork.course.service.QuestionService;
import com.pxwork.course.service.UserCourseEnrollmentService;
import com.pxwork.course.service.UserExamAnswerService;
import com.pxwork.course.service.UserExamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Tag(name = "4.6 前台-考试作答")
@RestController
@RequestMapping("/frontend/user-exams")
public class FrontendUserExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private UserExamService userExamService;

    @Autowired
    private UserCourseEnrollmentService userCourseEnrollmentService;

    @Autowired
    private ExamQuestionService examQuestionService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserExamAnswerService userExamAnswerService;

    @Autowired
    private AsyncAiGradingService asyncAiGradingService;

    @Operation(summary = "开始考试")
    @PostMapping
    public Result<Map<String, Object>> start(@RequestBody @Validated StartExamRequest request) {
        long userId = StpUserUtil.getLoginIdAsLong();
        Exam exam = examService.getById(request.getExamId());
        if (exam == null) {
            return Result.fail("考试不存在");
        }
        long enrolled = userCourseEnrollmentService.count(new LambdaQueryWrapper<UserCourseEnrollment>()
                .eq(UserCourseEnrollment::getUserId, userId)
                .eq(UserCourseEnrollment::getCourseId, exam.getCourseId()));
        if (enrolled == 0) {
            return Result.fail("无考试权限，请先选课");
        }
        List<UserExam> inProgressList = userExamService.list(new LambdaQueryWrapper<UserExam>()
                .eq(UserExam::getUserId, userId)
                .eq(UserExam::getExamId, request.getExamId())
                .eq(UserExam::getStatus, 0)
                .orderByDesc(UserExam::getId));
        UserExam userExam;
        if (inProgressList.isEmpty()) {
            userExam = new UserExam();
            userExam.setUserId(userId);
            userExam.setCourseId(exam.getCourseId());
            userExam.setExamId(request.getExamId());
            userExam.setStatus(0);
            userExam.setStartTime(LocalDateTime.now());
            userExam.setObjectiveScore(BigDecimal.ZERO);
            userExam.setSubjectiveScore(BigDecimal.ZERO);
            userExam.setPracticalScore(BigDecimal.ZERO);
            userExam.setFinalScore(BigDecimal.ZERO);
            userExam.setIsPassed(0);
            userExam.setMakeUpCount(0);
            userExamService.save(userExam);
        } else {
            userExam = inProgressList.get(0);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("userExamId", userExam.getId());
        result.put("examId", userExam.getExamId());
        return Result.success(result);
    }

    @Operation(summary = "获取试卷题目")
    @GetMapping("/{id}/questions")
    public Result<List<UserExamQuestionVO>> questions(@PathVariable Long id) {
        long userId = StpUserUtil.getLoginIdAsLong();
        UserExam userExam = userExamService.getById(id);
        if (userExam == null || !userExam.getUserId().equals(userId)) {
            return Result.fail("考试记录不存在");
        }
        List<ExamQuestion> examQuestions = examQuestionService.list(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, userExam.getExamId())
                .orderByAsc(ExamQuestion::getSort));
        if (examQuestions.isEmpty()) {
            return Result.success(List.of());
        }
        Set<Long> questionIds = examQuestions.stream().map(ExamQuestion::getQuestionId).collect(Collectors.toSet());
        List<Question> questionList = questionService.list(new LambdaQueryWrapper<Question>().in(Question::getId, questionIds));
        Map<Long, Question> questionMap = questionList.stream().collect(Collectors.toMap(Question::getId, q -> q));
        List<UserExamQuestionVO> result = new ArrayList<>();
        for (ExamQuestion examQuestion : examQuestions) {
            Question question = questionMap.get(examQuestion.getQuestionId());
            if (question == null) {
                continue;
            }
            UserExamQuestionVO vo = new UserExamQuestionVO();
            vo.setQuestionId(question.getId());
            vo.setQuestionType(question.getQuestionType());
            vo.setContent(question.getContent());
            vo.setOptions(question.getOptions());
            vo.setAnalysis(question.getAnalysis());
            vo.setScore(examQuestion.getScore());
            vo.setSort(examQuestion.getSort());
            result.add(vo);
        }
        return Result.success(result);
    }

    @Operation(summary = "交卷")
    @PostMapping("/{id}/submit")
    public Result<Map<String, Object>> submit(@PathVariable Long id, @RequestBody @Validated SubmitExamRequest request) {
        long userId = StpUserUtil.getLoginIdAsLong();
        UserExam userExam = userExamService.getById(id);
        if (userExam == null || !userExam.getUserId().equals(userId)) {
            return Result.fail("考试记录不存在");
        }
        if (userExam.getStatus() != null && userExam.getStatus() != 0) {
            return Result.fail("当前考试状态不可交卷");
        }
        List<ExamQuestion> examQuestions = examQuestionService.list(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, userExam.getExamId()));
        if (examQuestions.isEmpty()) {
            return Result.fail("试卷未配置题目");
        }
        Map<Long, ExamQuestion> examQuestionMap = examQuestions.stream()
                .collect(Collectors.toMap(ExamQuestion::getQuestionId, item -> item, (a, b) -> a));
        Set<Long> answerQuestionIds = request.getAnswers().stream().map(AnswerItem::getQuestionId).collect(Collectors.toSet());
        List<Question> questions = questionService.list(new LambdaQueryWrapper<Question>().in(Question::getId, answerQuestionIds));
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        List<UserExamAnswer> answerDetails = new ArrayList<>();
        BigDecimal objectiveScore = BigDecimal.ZERO;
        int subjectiveCount = 0;
        for (AnswerItem answer : request.getAnswers()) {
            ExamQuestion examQuestion = examQuestionMap.get(answer.getQuestionId());
            Question question = questionMap.get(answer.getQuestionId());
            if (examQuestion == null || question == null) {
                continue;
            }
            BigDecimal questionScore = examQuestion.getScore() == null ? BigDecimal.ZERO : examQuestion.getScore();
            String studentAnswer = answer.getUserAnswer() == null ? "" : answer.getUserAnswer().trim();
            UserExamAnswer answerDetail = new UserExamAnswer();
            answerDetail.setUserExamId(userExam.getId());
            answerDetail.setQuestionId(answer.getQuestionId());
            answerDetail.setUserAnswer(answer.getUserAnswer());
            if (isObjectiveQuestion(question)) {
                String standard = question.getStandardAnswer() == null ? "" : question.getStandardAnswer().trim();
                boolean correct = standard.equalsIgnoreCase(studentAnswer);
                if (correct) {
                    objectiveScore = objectiveScore.add(questionScore);
                }
                answerDetail.setIsCorrect(correct ? 1 : 0);
                answerDetail.setScore(correct ? questionScore : BigDecimal.ZERO);
            } else {
                answerDetail.setIsCorrect(null);
                answerDetail.setScore(BigDecimal.ZERO);
                subjectiveCount++;
            }
            answerDetails.add(answerDetail);
        }
        userExamAnswerService.remove(new LambdaQueryWrapper<UserExamAnswer>().eq(UserExamAnswer::getUserExamId, userExam.getId()));
        if (!answerDetails.isEmpty()) {
            userExamAnswerService.saveBatch(answerDetails);
        }
        userExam.setObjectiveScore(objectiveScore);
        userExam.setStatus(1);
        userExam.setSubmitTime(LocalDateTime.now());
        userExamService.updateById(userExam);
        if (subjectiveCount > 0) {
            asyncAiGradingService.gradeSubjectiveAnswers(userExam.getId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userExamId", userExam.getId());
        result.put("objectiveScore", objectiveScore);
        result.put("subjectiveCount", subjectiveCount);
        return Result.success(result);
    }

    private boolean isObjectiveQuestion(Question question) {
        if (StringUtils.hasText(question.getOptions())) {
            return true;
        }
        String type = question.getQuestionType();
        if (!StringUtils.hasText(type)) {
            return false;
        }
        String normalized = type.trim().toLowerCase();
        return "single_choice".equals(normalized)
                || "multiple_choice".equals(normalized)
                || "true_false".equals(normalized)
                || "judge".equals(normalized)
                || "判断题".equals(type.trim());
    }

    @Data
    public static class StartExamRequest {
        @NotNull(message = "考试ID不能为空")
        private Long examId;
    }

    @Data
    public static class SubmitExamRequest {
        @NotEmpty(message = "答案列表不能为空")
        private List<AnswerItem> answers;
    }

    @Data
    public static class AnswerItem {
        @NotNull(message = "题目ID不能为空")
        private Long questionId;
        private String userAnswer;
    }

    @Data
    public static class UserExamQuestionVO {
        private Long questionId;
        private String questionType;
        private String content;
        private String options;
        private String analysis;
        private BigDecimal score;
        private Integer sort;
    }
}
