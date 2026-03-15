package com.pxwork.api.controller.backend;

import java.util.ArrayList;
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
import com.pxwork.course.entity.Question;
import com.pxwork.course.service.QuestionService;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
            queryWrapper.eq(Question::getJobRoleTag, jobRoleTag);
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
            Map<String, Object> inputs = Map.of("job_role", jobRoleTag);
            String aiOutputJson = difyApiService.runGenerateWorkflow(inputs, fileId);
            List<JSONObject> aiQuestionItems = parseAiQuestionItems(aiOutputJson);
            if (aiQuestionItems.isEmpty()) {
                return Result.fail("AI 未生成可导入题目");
            }

            List<Question> questions = new ArrayList<>();
            for (JSONObject item : aiQuestionItems) {
                String content = item.getStr("content");
                if (!StringUtils.hasText(content)) {
                    continue;
                }
                Question question = new Question();
                question.setCategoryId(categoryId);
                question.setJobRoleTag(jobRoleTag);
                question.setQuestionType(item.getStr("question_type"));
                question.setContent(content);
                question.setOptions(toJsonStringOrText(item.get("options")));
                question.setStandardAnswer(toJsonStringOrText(item.get("standard_answer")));
                question.setAnalysis(item.getStr("analysis"));
                questions.add(question);
            }
            if (questions.isEmpty()) {
                return Result.fail("AI 结果缺少有效题目内容");
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
        return Result.success(questionService.removeById(id));
    }

    private List<JSONObject> parseAiQuestionItems(String aiOutputJson) {
        List<JSONObject> result = new ArrayList<>();
        if (!StringUtils.hasText(aiOutputJson)) {
            return result;
        }

        Object parsed = JSONUtil.parse(aiOutputJson);
        if (parsed instanceof JSONArray jsonArray) {
            for (Object item : jsonArray) {
                result.add(JSONUtil.parseObj(item));
            }
            return result;
        }

        if (parsed instanceof JSONObject jsonObject) {
            Object listNode = jsonObject.get("questions");
            if (listNode == null) {
                listNode = jsonObject.get("list");
            }
            if (listNode == null) {
                listNode = jsonObject.get("items");
            }
            if (listNode instanceof JSONArray listArray) {
                for (Object item : listArray) {
                    result.add(JSONUtil.parseObj(item));
                }
                return result;
            }
            result.add(jsonObject);
        }
        return result;
    }

    private String toJsonStringOrText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        return JSONUtil.toJsonStr(value);
    }
}
