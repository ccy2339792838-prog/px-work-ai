package com.pxwork.common.mapper;

import com.pxwork.common.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 学员用户表 Mapper 接口
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
