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
import java.util.List;

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
     * 邮箱(登录账号)
     */
    private String email;

    /**
     * 密码
     */
    private String password;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<Long> departmentIds;
    
    @TableField(exist = false)
    private List<Department> departments;
}
