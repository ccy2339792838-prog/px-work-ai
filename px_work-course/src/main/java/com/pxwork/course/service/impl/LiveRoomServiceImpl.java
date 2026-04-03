package com.pxwork.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.LiveRoom;
import com.pxwork.course.mapper.LiveRoomMapper;
import com.pxwork.course.service.LiveRoomService;
import org.springframework.stereotype.Service;

@Service
public class LiveRoomServiceImpl extends ServiceImpl<LiveRoomMapper, LiveRoom> implements LiveRoomService {

    @Override
    public LiveRoom getOrCreateRoom(Long hourId, String hourName) {
        // 1. 先去数据库查，这个课时是不是已经有房间了？
        LiveRoom room = this.getOne(new LambdaQueryWrapper<LiveRoom>()
                .eq(LiveRoom::getHourId, hourId));

        // 2. 如果有，直接把房间信息返回
        if (room != null) {
            return room;
        }

        // 3. 如果没有，说明是第一次点进这个直播课时，咱们现场给它建一个房间！
        room = new LiveRoom();
        room.setHourId(hourId);
        room.setRoomName(hourName + " 的直播间");
        // 🔴 核心：生成全局唯一的房间号，用 "room_hour_" + 课时ID，绝对不会重复！
        room.setRoomNo("room_hour_" + hourId); 
        room.setStatus(0); // 0代表未开始
        
        // 保存到数据库
        this.save(room);

        return room;
    }
}