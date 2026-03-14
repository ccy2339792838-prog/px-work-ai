package com.pxwork.api.controller.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pxwork.common.utils.Result;
import com.pxwork.resource.entity.Resource;
import com.pxwork.resource.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 资源库 前端控制器
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Tag(name = "资源库管理", description = "资源库相关的接口")
@RestController
@RequestMapping("/resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @Operation(summary = "资源分页列表", description = "获取资源分页列表")
    @GetMapping("/list")
    public Result<Page<Resource>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name) {
        
        Page<Resource> page = new Page<>(current, size);
        LambdaQueryWrapper<Resource> queryWrapper = new LambdaQueryWrapper<>();
        
        if (categoryId != null && categoryId > 0) {
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
        // Only update name
        Resource updateEntity = new Resource();
        updateEntity.setId(resource.getId());
        updateEntity.setName(resource.getName());
        
        boolean success = resourceService.updateById(updateEntity);
        return success ? Result.success(true) : Result.fail("更新失败");
    }

    @Operation(summary = "删除资源", description = "根据ID删除资源")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = resourceService.removeById(id);
        // TODO: Also delete physical file
        return success ? Result.success(true) : Result.fail("删除失败");
    }
}
