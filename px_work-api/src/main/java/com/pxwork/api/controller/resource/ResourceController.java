package com.pxwork.api.controller.resource;

import java.io.File;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pxwork.common.utils.Result;
import com.pxwork.resource.entity.Resource;
import com.pxwork.resource.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 资源库 前端控制器
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Tag(name = "2.5 后台-素材资源管理")
@RestController
@RequestMapping("/resource")
public class ResourceController {

    private static final Logger log = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    private ResourceService resourceService;

    @Value("${app.upload-dir:D:/px/backend/uploads}")
    private String uploadDir;

    @Operation(summary = "资源分页列表", description = "获取资源分页列表")
    @GetMapping("/list")
    public Result<Page<Resource>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name) {
        
        Page<Resource> page = new Page<>(current, size);
        LambdaQueryWrapper<Resource> queryWrapper = new LambdaQueryWrapper<>();
        
        if (categoryId != null) {
            queryWrapper.eq(Resource::getCategoryId, categoryId);
        }
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(Resource::getName, name);
        }
        
        queryWrapper.orderByDesc(Resource::getCreatedAt);
        
        return Result.success(resourceService.page(page, queryWrapper));
    }

    @Operation(summary = "重命名资源", description = "更新资源名称")
    @PutMapping("/rename")
    public Result<Boolean> rename(@RequestBody Resource resource) {
        if (resource.getId() == null || StringUtils.isBlank(resource.getName())) {
            return Result.fail("参数错误");
        }
        Resource updateEntity = new Resource();
        updateEntity.setId(resource.getId());
        updateEntity.setName(resource.getName());
        
        boolean success = resourceService.updateById(updateEntity);
        return success ? Result.success(true) : Result.fail("更新失败");
    }

    @Operation(summary = "批量移动素材")
    @PutMapping("/move")
    public Result<Boolean> move(@RequestBody MoveResourceRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty() || request.getTargetCategoryId() == null) {
            return Result.fail("参数错误");
        }
        boolean success = resourceService.moveResources(request.getIds(), request.getTargetCategoryId());
        return success ? Result.success(true) : Result.fail("移动失败");
    }

    @Operation(summary = "删除资源", description = "根据ID删除资源")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        Resource resource = resourceService.getById(id);
        if (resource == null) {
            return Result.fail("资源不存在");
        }
        try {
            String url = resource.getUrl();
            if (StringUtils.isNotBlank(url)) {
                String normalizedUrl = url.split("\\?")[0];
                int lastSlashIndex = normalizedUrl.lastIndexOf('/');
                String fileName = lastSlashIndex >= 0 ? normalizedUrl.substring(lastSlashIndex + 1) : normalizedUrl;
                if (StringUtils.isNotBlank(fileName)) {
                    File localFile = new File(uploadDir, fileName);
                    if (localFile.exists()) {
                        localFile.delete();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("删除资源物理文件失败, id={}", id, e);
        }
        boolean success = resourceService.removeById(id);
        return success ? Result.success(true) : Result.fail("删除失败");
    }

    public static class MoveResourceRequest {
        private List<Long> ids;
        private Long targetCategoryId;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }

        public Long getTargetCategoryId() {
            return targetCategoryId;
        }

        public void setTargetCategoryId(Long targetCategoryId) {
            this.targetCategoryId = targetCategoryId;
        }
    }
}
