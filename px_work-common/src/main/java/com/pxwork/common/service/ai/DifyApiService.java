package com.pxwork.common.service.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

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

    /**
     * 核心修复：使用 RestTemplate 原生上传文件，完美保留 Content-Type
     */
    public String uploadFile(MultipartFile file) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            // 声明这是一个包含文件的表单请求
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(generateKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            // 使用 getResource() 传递文件，Spring会自动处理 MIME Type
            body.add("file", file.getResource());
            body.add("user", USER);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // 发送 POST 请求到 Dify 上传接口
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + "/files/upload", requestEntity, String.class);
            
            JSONObject jsonObject = JSONUtil.parseObj(response.getBody());
            String id = jsonObject.getStr("id");
            if (id == null || id.isBlank()) {
                log.error("Dify upload response missing id, body={}", response.getBody());
                throw new RuntimeException("Dify 文件上传响应缺少 id");
            }
            log.info("✅ Dify 文件上传成功！FileId: {}", id);
            return id;
            
        } catch (RestClientResponseException e) {
            // 拦截 HTTP 报错，打印 Dify 返回的真实错误信息
            log.error("❌ Dify上传彻底失败! 状态码: {}, 返回体: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Dify上传失败: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("调用 Dify 文件上传发生异常", e);
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

            if (fileId != null && !fileId.isBlank()) {
                Map<String, Object> fileObj = new HashMap<>();
                fileObj.put("type", "document");
                fileObj.put("transfer_method", "local_file");
                fileObj.put("upload_file_id", fileId);
                // Dify 的变量名是 file，所以 key 是 "file"
                inputs.put("file", fileObj);
            }

            requestBody.put("inputs", inputs);
            requestBody.put("response_mode", "blocking");
            requestBody.put("user", USER);
            log.info("Dify workflow request prepared, inputKeys={}, hasFile={}", new ArrayList<>(inputs.keySet()), inputs.containsKey("file"));

            try (HttpResponse response = HttpRequest.post(baseUrl + "/workflows/run")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", ContentType.JSON.getValue())
                    .body(JSONUtil.toJsonStr(requestBody))
                    .execute()) {
                String body = response.body();
                if (!response.isOk()) {
                    log.error("Dify workflow failed, status={}, body={}", response.getStatus(), body);
                    throw new RuntimeException("调用 Dify 工作流失败，Dify的原始报错是: " + body);
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