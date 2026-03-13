package com.pxwork.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 管理员-角色关联表
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("admin_user_role")
public class AdminUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long adminUserId;

    private Long roleId;
}
