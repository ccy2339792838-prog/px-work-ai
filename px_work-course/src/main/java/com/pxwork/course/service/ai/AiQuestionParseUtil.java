package com.pxwork.course.service.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pxwork.common.utils.JsonUtils;
import com.pxwork.course.entity.Question;

@Component
public class AiQuestionParseUtil {

    @Autowired
    private ObjectMapper objectMapper;

    // 🔴 这里的参数改成了 defaultCourseId
    public List<Question> parseQuestions(String aiRawJson, String jobRoleTag, Long defaultCourseId) throws Exception {
        String cleanedJson = JsonUtils.cleanMarkdownJson(aiRawJson);
        if (!StringUtils.hasText(cleanedJson)) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(cleanedJson);
        List<Question> result = new ArrayList<>();
        if (root.isArray()) {
            for (JsonNode node : root) {
                Question question = toQuestion(node, jobRoleTag, defaultCourseId);
                if (question != null) {
                    result.add(question);
                }
            }
            return result;
        }
        JsonNode itemsNode = root.get("questions");
        if (itemsNode == null || !itemsNode.isArray()) {
            itemsNode = root.get("list");
        }
        if (itemsNode == null || !itemsNode.isArray()) {
            itemsNode = root.get("items");
        }
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode node : itemsNode) {
                Question question = toQuestion(node, jobRoleTag, defaultCourseId);
                if (question != null) {
                    result.add(question);
                }
            }
            return result;
        }
        Question single = toQuestion(root, jobRoleTag, defaultCourseId);
        if (single != null) {
            result.add(single);
        }
        return result;
    }

    // 🔴 这里的参数也改成了 defaultCourseId
    private Question toQuestion(JsonNode node, String jobRoleTag, Long defaultCourseId) throws Exception {
        if (node == null || node.isNull()) {
            return null;
        }
        String content = readText(node, "content");
        if (!StringUtils.hasText(content)) {
            return null;
        }
        Question question = new Question();
        question.setJobRoleTag(jobRoleTag);
        question.setQuestionType(normalizeQuestionType(readText(node, "question_type", "questionType")));
        question.setContent(content);
        question.setStandardAnswer(readText(node, "standard_answer", "standardAnswer"));
        question.setAnalysis(readText(node, "analysis"));

        // 🔴 这里的核心逻辑全改成了 courseId 相关的解析
        Long courseId = defaultCourseId;
        String courseText = readText(node, "course_id", "courseId");
        if (StringUtils.hasText(courseText)) {
            try {
                courseId = Long.parseLong(courseText);
            } catch (NumberFormatException ignored) {
            }
        }
        question.setCourseId(courseId); // 🔴 最终把解析出来的 courseId 存入实体对象

        JsonNode optionsNode = node.get("options");
        if (optionsNode != null && !optionsNode.isNull()) {
            if (optionsNode.isTextual()) {
                question.setOptions(optionsNode.asText());
            } else {
                question.setOptions(objectMapper.writeValueAsString(optionsNode));
            }
        }
        return question;
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
}