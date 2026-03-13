package com.pxwork.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 课程课时
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course_hours")
public class CourseHour implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 课时名称
     */
    private String name;

    /**
     * 类型: 1-视频, 2-图文
     */
    private Integer type;

    /**
     * 关联资源ID
     */
    private Long resourceId;

    /**
     * 时长(秒)
     */
    private Integer duration;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 图文内容
     */
    private String content;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
