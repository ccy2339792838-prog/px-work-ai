package com.pxwork.resource.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pxwork.resource.entity.Resource;

/**
 * <p>
 * 资源库表 服务类
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
public interface ResourceService extends IService<Resource> {

    boolean moveResources(List<Long> ids, Long targetCategoryId);
}
