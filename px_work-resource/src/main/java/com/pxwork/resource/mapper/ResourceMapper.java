package com.pxwork.resource.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pxwork.resource.entity.Resource;

/**
 * <p>
 * 资源库表 Mapper 接口
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {

}
