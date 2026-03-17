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
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
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
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pxwork.common.service.ai.DifyApiService;
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

    @Autowired
    private DifyApiService difyApiService;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Operation(summary = "手动绑定试卷题目")
    @PostMapping("/exams/{id}/bind-questions")
    public Result<Map<String, Object>> bindQuestions(@PathVariable Long id, @RequestBody List<Long> questionIds) {
        try {
            Map<String, Object> result = examQuestionService.bindQuestions(id, questionIds);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
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
            if (course != null && StringUtils.hasText(course.getTargetRoles())) {
                String[] roles = course.getTargetRoles().split(",");
                queryWrapper.and(wrapper -> {
                    boolean added = false;
                    for (String role : roles) {
                        if (StringUtils.hasText(role)) {
                            if (added) {
                                wrapper.or();
                            }
                            wrapper.apply("FIND_IN_SET({0}, job_role_tag)", role.trim());
                            added = true;
                        }
                    }
                });
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

    @Operation(summary = "AI一键出卷")
    @PostMapping(value = "/exams/ai-generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> aiGenerate(@RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId,
            @RequestParam("title") String title,
            @RequestParam("jobRoleTag") String jobRoleTag,
            @RequestParam(value = "questionConfig", defaultValue = "{\"单选\":5}") String questionConfigJson) {
        if (file == null || file.isEmpty()) {
            return Result.fail("课件文档不能为空");
        }
        if (courseId == null || courseId <= 0) {
            return Result.fail("课程ID不能为空");
        }
        if (!StringUtils.hasText(title)) {
            return Result.fail("试卷名称不能为空");
        }
        if (!StringUtils.hasText(jobRoleTag)) {
            return Result.fail("岗位要求不能为空");
        }
        if (courseService.getById(courseId) == null) {
            return Result.fail("课程不存在");
        }
        try {
            Map<String, Integer> configMap = objectMapper.readValue(questionConfigJson, new TypeReference<Map<String, Integer>>() {
            });
            if (configMap == null || configMap.isEmpty()) {
                return Result.fail("题型数量配置不能为空");
            }
            StringBuilder requirementsBuilder = new StringBuilder();
            int totalExpected = 0;
            for (Map.Entry<String, Integer> entry : configMap.entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    requirementsBuilder.append("【").append(entry.getKey()).append("】").append(entry.getValue()).append("道，");
                    totalExpected += entry.getValue();
                }
            }
            if (totalExpected == 0) {
                return Result.fail("题目总数不能为0");
            }
            String questionRequirements = requirementsBuilder.toString();

            String fileId = difyApiService.uploadFile(file);
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("job_roles", jobRoleTag);
            inputs.put("question_requirements", questionRequirements);
            String aiRawJson = difyApiService.runGenerateWorkflow(inputs, fileId);
            System.out.println("====== AI 原始返回数据 ======");
            System.out.println(aiRawJson);
            System.out.println("===========================");
            List<Question> questions = parseAiQuestions(aiRawJson, jobRoleTag);
            if (questions.isEmpty()) {
                return Result.fail("AI未生成有效题目");
            }

            boolean questionSaved = questionService.saveBatch(questions);
            if (!questionSaved) {
                return Result.fail("题库入库失败");
            }

            Exam exam = new Exam();
            exam.setCourseId(courseId);
            exam.setTitle(title);
            exam.setDuration(90);
            exam.setWeightProcess(new BigDecimal("0.30"));
            exam.setWeightEnd(new BigDecimal("0.70"));
            exam.setWeightPractical(BigDecimal.ZERO);
            exam.setPassTotalScore(new BigDecimal("60"));
            exam.setPassProcessScore(BigDecimal.ZERO);
            exam.setPassEndScore(new BigDecimal("60"));
            exam.setPassPracticalScore(BigDecimal.ZERO);
            boolean examSaved = examService.save(exam);
            if (!examSaved || exam.getId() == null) {
                return Result.fail("试卷创建失败");
            }

            List<ExamQuestion> examQuestions = new ArrayList<>();
            int sort = 1;
            for (Question question : questions) {
                if (question.getId() == null) {
                    return Result.fail("题目入库后缺少ID，无法组装试卷");
                }
                ExamQuestion examQuestion = new ExamQuestion();
                examQuestion.setExamId(exam.getId());
                examQuestion.setQuestionId(question.getId());
                examQuestion.setScore(new BigDecimal("10"));
                examQuestion.setSort(sort++);
                examQuestions.add(examQuestion);
            }
            if (!examQuestions.isEmpty()) {
                boolean relationSaved = examQuestionService.saveBatch(examQuestions);
                if (!relationSaved) {
                    return Result.fail("试卷题目关联保存失败");
                }
            }
            return Result.success("AI出卷成功，请在列表审阅微调", exam.getId());
        } catch (Exception e) {
            return Result.fail("AI出卷失败: " + e.getMessage());
        }
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

    private List<Question> parseAiQuestions(String aiRawJson, String jobRoleTag) throws Exception {
        if (!StringUtils.hasText(aiRawJson)) {
            return List.of();
        }

        String jsonText = aiRawJson.replaceAll("```json", "").replaceAll("```", "").trim();
        jsonText = jsonText.replaceAll("\\]\\s*\\[", "],[");
        if (jsonText.startsWith("[") && jsonText.contains("],[")) {
            jsonText = "[" + jsonText + "]";
        }

        int startList = jsonText.indexOf('[');
        int endList = jsonText.lastIndexOf(']');
        int startObj = jsonText.indexOf('{');
        int endObj = jsonText.lastIndexOf('}');
        if (startList != -1 && endList != -1 && (startObj == -1 || startList < startObj)) {
            jsonText = jsonText.substring(startList, endList + 1);
        } else if (startObj != -1 && endObj != -1) {
            jsonText = jsonText.substring(startObj, endObj + 1);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(jsonText);
        } catch (Exception e) {
            return List.of();
        }

        Map<Long, Question> questionMap = new HashMap<>();
        processJsonNode(root, questionMap, jobRoleTag);

        List<Question> result = new ArrayList<>();
        for (Question q : questionMap.values()) {
            if (StringUtils.hasText(q.getContent())) {
                result.add(q);
            }
        }
        return result;
    }

    private void processJsonNode(JsonNode node, Map<Long, Question> questionMap, String jobRoleTag) throws Exception {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                processJsonNode(item, questionMap, jobRoleTag);
            }
        } else if (node.isObject()) {
            if (node.has("question") || node.has("content") || node.has("title")
                    || node.has("answer") || node.has("standard_answer")
                    || node.has("analysis") || node.has("explanation") || node.has("options")) {
                mergeToQuestion(node, questionMap, jobRoleTag);
            }

            java.util.Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode child = entry.getValue();

                if ("options".equalsIgnoreCase(key)) {
                    continue;
                }

                if (child.isArray() || child.isObject()) {
                    processJsonNode(child, questionMap, jobRoleTag);
                } else if (child.isTextual()) {
                    String text = child.asText().trim();
                    if ((text.startsWith("[") && text.endsWith("]"))
                            || (text.startsWith("{") && text.endsWith("}"))) {
                        try {
                            JsonNode parsedChild = objectMapper.readTree(text);
                            processJsonNode(parsedChild, questionMap, jobRoleTag);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }

    private void mergeToQuestion(JsonNode node, Map<Long, Question> questionMap, String jobRoleTag) throws Exception {
        Long id = null;
        if (node.has("id")) {
            id = node.get("id").asLong();
        }
        if (id == null || id <= 0) {
            id = (long) (questionMap.size() + 1000);
        }

        Question q = questionMap.getOrDefault(id, new Question());
        q.setJobRoleTag(jobRoleTag);
        if (q.getCategoryId() == null) {
            q.setCategoryId(0L);
        }

        String content = readText(node, "question", "content", "title");
        if (StringUtils.hasText(content)) {
            q.setContent(content);
        }

        String type = readText(node, "type", "question_type", "questionType");
        if (StringUtils.hasText(type)) {
            q.setQuestionType(normalizeQuestionType(type));
        } else if (q.getQuestionType() == null) {
            q.setQuestionType("short_answer");
        }

        String answer = readText(node, "answer", "standard_answer", "standardAnswer");
        if (StringUtils.hasText(answer)) {
            q.setStandardAnswer(answer);
        }

        String analysis = readText(node, "analysis", "explanation", "reason");
        if (StringUtils.hasText(analysis)) {
            q.setAnalysis(analysis);
        }

        JsonNode optionsNode = node.get("options");
        if (optionsNode != null && !optionsNode.isNull() && !optionsNode.isEmpty()) {
            if (optionsNode.isTextual()) {
                q.setOptions(optionsNode.asText());
            } else {
                q.setOptions(objectMapper.writeValueAsString(optionsNode));
            }
        }

        questionMap.put(id, q);
    }

    private String normalizeQuestionType(String rawType) {
        if (!StringUtils.hasText(rawType)) {
            return "short_answer";
        }
        String type = rawType.trim().toLowerCase();
        return switch (type) {
            case "single_choice", "single", "单选", "单选题" -> "single_choice";
            case "short_answer", "short", "subjective", "简答", "简答题", "主观题" -> "short_answer";
            case "case_analysis", "case", "案例分析", "案例分析题" -> "case_analysis";
            case "practical_application", "practical", "实操", "实操题", "实操应用题" -> "practical_application";
            default -> type;
        };
    }

    private String readText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value == null || value.isNull()) {
                continue;
            }
            String text = value.isTextual() ? value.asText() : value.toString();
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
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
