package com.pxwork.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 直播间实体类
 */
@Data
@TableName("live_rooms")
public class LiveRoom {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 关联的课时ID
    private Long hourId;
    
    // 直播间名称
    private String roomName;
    
    // 给腾讯云SDK用的全局唯一房间号(比如: room_1024)
    private String roomNo;
    
    // 状态: 0-未开始, 1-直播中, 2-已结束
    private Integer status;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}