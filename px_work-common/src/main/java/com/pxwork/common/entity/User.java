package com.pxwork.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 学员用户表
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 学员姓名
     */
    private String name;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 身份证号(登录账号)
     */
    private String idCard;

    /**
     * 所属企业
     */
    private String enterprise;

    /**
     * 岗位(关联字典表value)
     */
    private String jobRole;

    /**
     * 所属行业(关联字典表value)
     */
    private String industry;

    /**
     * 邮箱(登录账号)
     */
    private String email;

    /**
     * 密码
     */
    private String password;

    /**
     * 是否首次登录 1:是 0:否
     */
    private Integer isFirstLogin;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Long departmentId;
    
    @TableField(exist = false)
    private Department department;
}
