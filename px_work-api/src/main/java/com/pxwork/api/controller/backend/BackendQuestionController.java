package com.pxwork.api.controller.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
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
import com.pxwork.common.service.ai.DifyApiService;
import com.pxwork.common.utils.Result;
import com.pxwork.course.entity.ExamQuestion;
import com.pxwork.course.entity.Question;
import com.pxwork.course.entity.UserExamAnswer;
import com.pxwork.course.service.ExamQuestionService;
import com.pxwork.course.service.QuestionService;
import com.pxwork.course.service.UserExamAnswerService;
import com.pxwork.course.service.ai.AiQuestionParseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "3.3 后台-题库管理")
@RestController
@RequestMapping("/backend/questions")
public class BackendQuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private DifyApiService difyApiService;

    @Autowired
    private AiQuestionParseUtil aiQuestionParseUtil;

    @Autowired
    private ExamQuestionService examQuestionService;

    @Autowired
    private UserExamAnswerService userExamAnswerService;

    @Operation(summary = "题目分页列表")
    @GetMapping
    public Result<Page<Question>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String industryTag,
            @RequestParam(required = false) String jobRoleTag,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String content) {
        Page<Question> page = new Page<>(current, size);
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(questionType)) {
            queryWrapper.eq(Question::getQuestionType, questionType);
        }
        if (StringUtils.hasText(industryTag)) {
            queryWrapper.eq(Question::getIndustryTag, industryTag);
        }
        if (StringUtils.hasText(jobRoleTag)) {
            queryWrapper.apply("FIND_IN_SET({0}, job_role_tag)", jobRoleTag.trim());
        }
        if (categoryId != null && categoryId > 0) {
            queryWrapper.eq(Question::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(content)) {
            queryWrapper.like(Question::getContent, content);
        }
        queryWrapper.orderByDesc(Question::getCreatedAt);
        return Result.success(questionService.page(page, queryWrapper));
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{id}")
    public Result<Question> detail(@PathVariable Long id) {
        Question question = questionService.getById(id);
        if (question == null) {
            return Result.fail("题目不存在");
        }
        return Result.success(question);
    }

    @Operation(summary = "创建题目")
    @PostMapping
    public Result<Boolean> create(@RequestBody Question question) {
        return Result.success(questionService.save(question));
    }

    @Operation(summary = "AI 文档自动出题并入库")
    @PostMapping(value = "/ai-generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> aiGenerate(@RequestParam("file") MultipartFile file,
            @RequestParam("jobRoleTag") String jobRoleTag,
            @RequestParam("categoryId") Long categoryId) {
        if (file == null || file.isEmpty()) {
            return Result.fail("文件不能为空");
        }
        if (!StringUtils.hasText(jobRoleTag)) {
            return Result.fail("岗位标签不能为空");
        }
        if (categoryId == null || categoryId <= 0) {
            return Result.fail("题目分类不能为空");
        }
        try {
            String fileId = difyApiService.uploadFile(file);
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("job_roles", jobRoleTag);
            inputs.put("question_count", 5);
            String aiOutputJson = difyApiService.runGenerateWorkflow(inputs, fileId);
            List<Question> questions = aiQuestionParseUtil.parseQuestions(aiOutputJson, jobRoleTag, categoryId);
            if (questions.isEmpty()) {
                return Result.fail("AI 未生成可导入题目");
            }

            boolean saved = questionService.saveBatch(questions);
            if (!saved) {
                return Result.fail("题目批量入库失败");
            }
            return Result.success("成功导入题目数量: " + questions.size(), Map.of("importedCount", questions.size()));
        } catch (Exception e) {
            return Result.fail("AI 出题失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新题目")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Question question) {
        if (questionService.getById(id) == null) {
            return Result.fail("题目不存在");
        }
        question.setId(id);
        return Result.success(questionService.updateById(question));
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (questionService.getById(id) == null) {
            return Result.fail("题目不存在");
        }
        long examRefCount = examQuestionService.count(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getQuestionId, id));
        if (examRefCount > 0) {
            return Result.fail("该题目已被试卷引用，无法直接删除");
        }
        long answerRefCount = userExamAnswerService.count(new LambdaQueryWrapper<UserExamAnswer>()
                .eq(UserExamAnswer::getQuestionId, id));
        if (answerRefCount > 0) {
            return Result.fail("该题目已有学员作答记录，无法直接删除");
        }
        return Result.success(questionService.removeById(id));
    }

    @Operation(summary = "查询指定试卷绑定的所有题目")
    @GetMapping("/exam/{examId}")
    public Result<List<Map<String, Object>>> getQuestionsByExamId(@PathVariable Long examId) {
        // 1. 去关联表查出题目ID
        List<ExamQuestion> examQuestions = examQuestionService.list(
                new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .orderByAsc(ExamQuestion::getSort)
        );

        if (examQuestions == null || examQuestions.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }

        List<Long> questionIds = examQuestions.stream()
                .map(ExamQuestion::getQuestionId)
                .collect(java.util.stream.Collectors.toList());

        // 2. 查出完整的题目对象
        List<Question> questions = questionService.listByIds(questionIds);

        // 3. 把 Question 对象伪装成 Map，故意不传 categoryId 给前端
        List<Map<String, Object>> resultList = questions.stream().map(q -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", q.getId());
            map.put("questionType", q.getQuestionType());
            map.put("content", q.getContent());
            map.put("options", q.getOptions());
            map.put("standardAnswer", q.getStandardAnswer());
            map.put("analysis", q.getAnalysis());
            map.put("industryTag", q.getIndustryTag());
            map.put("jobRoleTag", q.getJobRoleTag());
            map.put("createdAt", q.getCreatedAt());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        // 4. 返回精简后的数据
        return Result.success(resultList);
    }
}