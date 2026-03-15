package com.pxwork.common.service.ai;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DifyApiService {

    private static final String USER = "px_work_system";

    @Value("${ai.dify.base-url}")
    private String baseUrl;

    @Value("${ai.dify.generate-key}")
    private String generateKey;

    @Value("${ai.dify.grade-key}")
    private String gradeKey;

    public String uploadFile(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             HttpResponse response = HttpRequest.post(baseUrl + "/files/upload")
                     .header("Authorization", "Bearer " + generateKey)
                     .form("file", inputStream, file.getOriginalFilename())
                     .form("user", USER)
                     .execute()) {
            String body = response.body();
            if (!response.isOk()) {
                log.error("Dify upload failed, status={}, body={}", response.getStatus(), body);
                throw new RuntimeException("调用 Dify 文件上传失败");
            }
            JSONObject jsonObject = JSONUtil.parseObj(body);
            String id = jsonObject.getStr("id");
            if (id == null || id.isBlank()) {
                log.error("Dify upload response missing id, body={}", body);
                throw new RuntimeException("Dify 文件上传响应缺少 id");
            }
            return id;
        } catch (IOException e) {
            throw new RuntimeException("读取上传文件失败", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("调用 Dify 文件上传失败", e);
        }
    }

    public String runGenerateWorkflow(Map<String, Object> inputs, String fileId) {
        return runWorkflowWithKey(inputs, fileId, generateKey);
    }

    public String runGradeWorkflow(Map<String, Object> inputs) {
        return runWorkflowWithKey(inputs, null, gradeKey);
    }

    private String runWorkflowWithKey(Map<String, Object> inputs, String fileId, String apiKey) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", inputs);
            requestBody.put("response_mode", "blocking");
            requestBody.put("user", USER);

            if (fileId != null && !fileId.isBlank()) {
                Map<String, Object> file = new HashMap<>();
                file.put("type", "document");
                file.put("transfer_method", "local_file");
                file.put("upload_file_id", fileId);
                List<Map<String, Object>> filesList = new ArrayList<>();
                filesList.add(file);
                requestBody.put("files", filesList);
            }

            try (HttpResponse response = HttpRequest.post(baseUrl + "/workflows/run")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", ContentType.JSON.getValue())
                    .body(JSONUtil.toJsonStr(requestBody))
                    .execute()) {
                String body = response.body();
                if (!response.isOk()) {
                    log.error("Dify workflow failed, status={}, body={}", response.getStatus(), body);
                    throw new RuntimeException("调用 Dify 工作流失败");
                }

                JSONObject responseObj;
                try {
                    JSON parsed = JSONUtil.parse(body);
                    if (!(parsed instanceof JSONObject jsonObject)) {
                        log.error("Dify workflow response is not JSON object, body={}", body);
                        throw new RuntimeException("Dify 工作流响应格式错误");
                    }
                    responseObj = jsonObject;
                } catch (RuntimeException ex) {
                    log.error("Dify workflow response parse error, body={}", body, ex);
                    throw new RuntimeException("Dify 工作流响应解析失败");
                }
                JSONObject data = responseObj.getJSONObject("data");
                if (data == null) {
                    log.error("Dify workflow response missing data, body={}", body);
                    throw new RuntimeException("Dify 工作流响应缺少 data");
                }

                Object outputs = data.get("outputs");
                if (outputs == null) {
                    log.error("Dify workflow response missing outputs, body={}", body);
                    throw new RuntimeException("Dify 工作流响应缺少 outputs");
                }
                if (outputs instanceof CharSequence sequence) {
                    return sequence.toString();
                }
                return JSONUtil.toJsonStr(outputs);
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("调用 Dify 工作流失败", e);
        }
    }
}
