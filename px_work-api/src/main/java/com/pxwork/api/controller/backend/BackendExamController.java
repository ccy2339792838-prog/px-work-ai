package com.pxwork.api.controller.backend;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pxwork.common.utils.Result;
import com.pxwork.course.entity.Course;
import com.pxwork.course.entity.Exam;
import com.pxwork.course.entity.ExamQuestion;
import com.pxwork.course.entity.Question;
import com.pxwork.course.entity.UserExam;
import com.pxwork.course.entity.UserExamAnswer;
import com.pxwork.course.service.CourseService;
import com.pxwork.course.service.ExamQuestionService;
import com.pxwork.course.service.ExamService;
import com.pxwork.course.service.QuestionService;
import com.pxwork.course.service.UserExamAnswerService;
import com.pxwork.course.service.UserExamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Tag(name = "3.4 后台-试卷与考试管理")
@RestController
@RequestMapping("/backend")
public class BackendExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private ExamQuestionService examQuestionService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserExamService userExamService;

    @Autowired
    private UserExamAnswerService userExamAnswerService;

    @Operation(summary = "考试分页列表")
    @GetMapping("/exams")
    public Result<Page<Exam>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String title) {
        Page<Exam> page = new Page<>(current, size);
        LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
        if (courseId != null && courseId > 0) {
            queryWrapper.eq(Exam::getCourseId, courseId);
        }
        if (StringUtils.hasText(title)) {
            queryWrapper.like(Exam::getTitle, title);
        }
        queryWrapper.orderByDesc(Exam::getCreatedAt);
        return Result.success(examService.page(page, queryWrapper));
    }

    @Operation(summary = "考试详情")
    @GetMapping("/exams/{id}")
    public Result<Exam> detail(@PathVariable Long id) {
        Exam exam = examService.getById(id);
        if (exam == null) {
            return Result.fail("考试不存在");
        }
        return Result.success(exam);
    }

    @Operation(summary = "创建考试")
    @PostMapping("/exams")
    public Result<Boolean> create(@RequestBody @Validated ExamRequest request) {
        if (courseService.getById(request.getCourseId()) == null) {
            return Result.fail("课程不存在");
        }
        Exam exam = toExam(request);
        return Result.success(examService.save(exam));
    }

    @Operation(summary = "更新考试")
    @PutMapping("/exams/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Validated ExamRequest request) {
        Exam exists = examService.getById(id);
        if (exists == null) {
            return Result.fail("考试不存在");
        }
        if (courseService.getById(request.getCourseId()) == null) {
            return Result.fail("课程不存在");
        }
        Exam exam = toExam(request);
        exam.setId(id);
        return Result.success(examService.updateById(exam));
    }

    @Operation(summary = "删除考试")
    @DeleteMapping("/exams/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (examService.getById(id) == null) {
            return Result.fail("考试不存在");
        }
        examQuestionService.remove(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id));
        return Result.success(examService.removeById(id));
    }

    @Operation(summary = "传统自动组卷")
    @PostMapping("/exams/{id}/auto-generate")
    public Result<Map<String, Object>> autoGenerate(@PathVariable Long id, @RequestBody Map<String, Integer> questionTypeCountMap) {
        Exam exam = examService.getById(id);
        if (exam == null) {
            return Result.fail("考试不存在");
        }
        if (questionTypeCountMap == null || questionTypeCountMap.isEmpty()) {
            return Result.fail("题型抽取配置不能为空");
        }
        Course course = courseService.getById(exam.getCourseId());
        String roleTag = null;
        if (course != null && StringUtils.hasText(course.getTargetRoles())) {
            String[] roles = course.getTargetRoles().split(",");
            if (roles.length > 0 && StringUtils.hasText(roles[0])) {
                roleTag = roles[0].trim();
            }
        }

        List<ExamQuestion> generated = new ArrayList<>();
        int sort = 1;
        for (Map.Entry<String, Integer> entry : questionTypeCountMap.entrySet()) {
            String questionType = entry.getKey();
            Integer count = entry.getValue();
            if (!StringUtils.hasText(questionType) || count == null || count <= 0) {
                continue;
            }
            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<Question>()
                    .eq(Question::getQuestionType, questionType);
            if (StringUtils.hasText(roleTag)) {
                queryWrapper.eq(Question::getJobRoleTag, roleTag);
            }
            List<Question> candidates = questionService.list(queryWrapper);
            if (candidates.size() < count) {
                return Result.fail("题型[" + questionType + "]可用题量不足");
            }
            Collections.shuffle(candidates);
            for (int i = 0; i < count; i++) {
                Question question = candidates.get(i);
                ExamQuestion examQuestion = new ExamQuestion();
                examQuestion.setExamId(id);
                examQuestion.setQuestionId(question.getId());
                examQuestion.setScore(BigDecimal.ONE);
                examQuestion.setSort(sort++);
                generated.add(examQuestion);
            }
        }
        examQuestionService.remove(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id));
        if (!generated.isEmpty()) {
            examQuestionService.saveBatch(generated);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("examId", id);
        result.put("questionCount", generated.size());
        return Result.success(result);
    }

    @Operation(summary = "AI组卷预留")
    @PostMapping("/exams/{id}/ai-generate")
    public Result<Boolean> aiGenerate(@PathVariable Long id, @RequestBody @Validated AiGenerateRequest request) {
        if (examService.getById(id) == null) {
            return Result.fail("考试不存在");
        }
        // 此处调用 AI 根据文本生成题目并入库
        return Result.success(true);
    }

    @Operation(summary = "待批改试卷详情")
    @GetMapping("/user-exams/{id}/grading-detail")
    public Result<Map<String, Object>> gradingDetail(@PathVariable Long id) {
        UserExam userExam = userExamService.getById(id);
        if (userExam == null) {
            return Result.fail("学员考试记录不存在");
        }
        List<UserExamAnswer> answerList = userExamAnswerService.list(new LambdaQueryWrapper<UserExamAnswer>()
                .eq(UserExamAnswer::getUserExamId, id));
        List<UserExamAnswer> subjectiveAnswers = answerList.stream()
                .filter(item -> item.getIsCorrect() == null)
                .collect(Collectors.toList());

        List<GradingQuestionDetailVO> details = new ArrayList<>();
        if (!subjectiveAnswers.isEmpty()) {
            Set<Long> questionIds = subjectiveAnswers.stream()
                    .map(UserExamAnswer::getQuestionId)
                    .collect(Collectors.toSet());
            List<Question> questionList = questionService.list(new LambdaQueryWrapper<Question>().in(Question::getId, questionIds));
            Map<Long, Question> questionMap = questionList.stream().collect(Collectors.toMap(Question::getId, item -> item));

            for (UserExamAnswer answer : subjectiveAnswers) {
                Question question = questionMap.get(answer.getQuestionId());
                if (question == null) {
                    continue;
                }
                GradingQuestionDetailVO vo = new GradingQuestionDetailVO();
                vo.setQuestionId(answer.getQuestionId());
                vo.setQuestion(question.getContent());
                vo.setStudentAnswer(answer.getUserAnswer());
                vo.setStandardAnswer(question.getStandardAnswer());
                vo.setAiScore(answer.getScore());
                vo.setAiComment(answer.getAiComment());
                vo.setTeacherComment(answer.getTeacherComment());
                details.add(vo);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userExamId", userExam.getId());
        result.put("examId", userExam.getExamId());
        result.put("subjectiveQuestions", details);
        return Result.success(result);
    }

    @Operation(summary = "提交主观题最终批改结果")
    @PutMapping("/user-exams/{id}/subjective-grade")
    public Result<Map<String, Object>> subjectiveGrade(@PathVariable Long id, @RequestBody @Validated SubjectiveGradeRequest request) {
        if (request.getUserExamId() == null || !id.equals(request.getUserExamId())) {
            return Result.fail("路径参数与请求中的考试记录ID不一致");
        }
        UserExam userExam = userExamService.getById(id);
        if (userExam == null) {
            return Result.fail("学员考试记录不存在");
        }

        Set<Long> questionIds = request.getItems().stream().map(SubjectiveGradeItem::getQuestionId).collect(Collectors.toSet());
        List<UserExamAnswer> storedAnswers = userExamAnswerService.list(new LambdaQueryWrapper<UserExamAnswer>()
                .eq(UserExamAnswer::getUserExamId, id)
                .in(UserExamAnswer::getQuestionId, questionIds));
        Map<Long, UserExamAnswer> answerMap = storedAnswers.stream()
                .collect(Collectors.toMap(UserExamAnswer::getQuestionId, item -> item, (a, b) -> a));

        List<UserExamAnswer> toUpdate = new ArrayList<>();
        BigDecimal subjectiveScore = BigDecimal.ZERO;
        for (SubjectiveGradeItem item : request.getItems()) {
            UserExamAnswer answer = answerMap.get(item.getQuestionId());
            if (answer == null) {
                return Result.fail("题目[" + item.getQuestionId() + "]答题记录不存在");
            }
            if (answer.getIsCorrect() != null) {
                return Result.fail("题目[" + item.getQuestionId() + "]不是主观题，无法人工批改");
            }
            answer.setScore(item.getScore());
            answer.setTeacherComment(item.getTeacherComment());
            toUpdate.add(answer);
            subjectiveScore = subjectiveScore.add(item.getScore());
        }

        if (!toUpdate.isEmpty()) {
            userExamAnswerService.updateBatchById(toUpdate);
        }

        userExam.setSubjectiveScore(subjectiveScore);
        boolean updated = userExamService.updateById(userExam);
        if (!updated) {
            return Result.fail("更新主观题成绩失败");
        }

        Exam exam = examService.getById(userExam.getExamId());
        BigDecimal practicalWeight = exam == null || exam.getWeightPractical() == null ? BigDecimal.ZERO : exam.getWeightPractical();
        if (practicalWeight.compareTo(BigDecimal.ZERO) == 0) {
            Map<String, Object> finalResult = userExamService.calculateFinalResult(id);
            finalResult.put("finalized", true);
            return Result.success(finalResult);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userExamId", userExam.getId());
        result.put("subjectiveScore", subjectiveScore);
        result.put("finalized", false);
        return Result.success(result);
    }

    @Operation(summary = "录入实操成绩")
    @PutMapping("/user-exams/{userExamId}/practical-grade")
    public Result<Map<String, Object>> practicalGrade(@PathVariable Long userExamId, @RequestBody @Validated PracticalGradeRequest request) {
        UserExam userExam = userExamService.getById(userExamId);
        if (userExam == null) {
            return Result.fail("学员考试记录不存在");
        }
        userExam.setPracticalScore(request.getPracticalScore());
        boolean updated = userExamService.updateById(userExam);
        if (!updated) {
            return Result.fail("更新实操成绩失败");
        }
        return Result.success(userExamService.calculateFinalResult(userExamId));
    }

    private Exam toExam(ExamRequest request) {
        Exam exam = new Exam();
        exam.setCourseId(request.getCourseId());
        exam.setTitle(request.getTitle());
        exam.setDuration(request.getDuration());
        exam.setWeightProcess(request.getWeightProcess());
        exam.setWeightEnd(request.getWeightEnd());
        exam.setWeightPractical(request.getWeightPractical());
        exam.setPassTotalScore(request.getPassTotalScore());
        exam.setPassProcessScore(request.getPassProcessScore());
        exam.setPassEndScore(request.getPassEndScore());
        exam.setPassPracticalScore(request.getPassPracticalScore());
        return exam;
    }

    @Data
    public static class ExamRequest {
        @NotNull(message = "课程ID不能为空")
        private Long courseId;
        @NotBlank(message = "考试标题不能为空")
        private String title;
        @NotNull(message = "考试时长不能为空")
        private Integer duration;
        @NotNull(message = "过程评价权重不能为空")
        private BigDecimal weightProcess;
        @NotNull(message = "终结考核权重不能为空")
        private BigDecimal weightEnd;
        @NotNull(message = "实操权重不能为空")
        private BigDecimal weightPractical;
        @NotNull(message = "综合合格总分不能为空")
        private BigDecimal passTotalScore;
        @NotNull(message = "过程评价合格分不能为空")
        private BigDecimal passProcessScore;
        @NotNull(message = "终结考核合格分不能为空")
        private BigDecimal passEndScore;
        @NotNull(message = "实操合格分不能为空")
        private BigDecimal passPracticalScore;
    }

    @Data
    public static class AiGenerateRequest {
        @NotBlank(message = "教材文本不能为空")
        private String materialText;
    }

    @Data
    public static class SubjectiveGradeRequest {
        @NotNull(message = "考试记录ID不能为空")
        private Long userExamId;
        @NotEmpty(message = "批改明细不能为空")
        @Valid
        private List<SubjectiveGradeItem> items;
    }

    @Data
    public static class SubjectiveGradeItem {
        @NotNull(message = "题目ID不能为空")
        private Long questionId;
        @NotNull(message = "分数不能为空")
        private BigDecimal score;
        private String teacherComment;
    }

    @Data
    public static class PracticalGradeRequest {
        @NotNull(message = "实操成绩不能为空")
        private BigDecimal practicalScore;
    }

    @Data
    public static class GradingQuestionDetailVO {
        private Long questionId;
        private String question;
        private String studentAnswer;
        private String standardAnswer;
        private BigDecimal aiScore;
        private String aiComment;
        private String teacherComment;
    }
}
