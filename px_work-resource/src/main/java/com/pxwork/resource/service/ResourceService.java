package com.pxwork.resource.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pxwork.resource.entity.Resource;
import java.util.List;

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
