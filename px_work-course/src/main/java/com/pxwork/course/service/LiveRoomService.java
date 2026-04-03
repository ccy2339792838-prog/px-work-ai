package com.pxwork.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pxwork.course.entity.LiveRoom;

/**
 * 直播间 Service 接口
 */
public interface LiveRoomService extends IService<LiveRoom> {
    
    /**
     * 根据课时ID获取直播间，如果不存在则自动创建一个
     */
    LiveRoom getOrCreateRoom(Long hourId, String hourName);
    
}