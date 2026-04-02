package com.pxwork.api.controller.resource;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType; // 🔴 新增导入
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pxwork.common.utils.Result;
import com.pxwork.resource.entity.Resource;
import com.pxwork.resource.service.ResourceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 文件上传 前端控制器
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Slf4j
@Tag(name = "2.5 后台-素材资源管理")
@RestController
@RequestMapping("/upload")
public class UploadController {

    @Autowired
    private ResourceService resourceService;

    @Value("${app.upload-dir:D:/px/backend/uploads}")
    private String uploadDir;

    @Operation(summary = "上传文件", description = "上传文件并保存到本地，返回URL")
    // 🔴 关键修复：在这里明确加上 consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Resource> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.fail("文件为空");
        }

        try {
            String savePath = uploadDir;
            File dir = new File(savePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + suffix;

            File dest = new File(savePath + File.separator + newFilename);
            file.transferTo(dest);

            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String contextPath = request.getContextPath();
            String url = scheme + "://" + serverName + ":" + serverPort + contextPath + "/uploads/" + newFilename;

            Resource resource = new Resource();
            resource.setName(originalFilename);
            resource.setType(file.getContentType());
            resource.setUrl(url);
            resource.setSize(file.getSize());
            resource.setDuration(0);
            resource.setCategoryId(0L);

            resourceService.save(resource);

            return Result.success(resource);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }
}