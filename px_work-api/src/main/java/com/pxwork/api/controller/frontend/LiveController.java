package com.pxwork.api.controller.frontend;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pxwork.common.utils.Result;
import com.pxwork.common.utils.StpUserUtil;
import com.pxwork.course.entity.CourseHour;
import com.pxwork.course.entity.LiveRoom;
import com.pxwork.course.service.CourseHourService;
import com.pxwork.course.service.LiveRoomService;
import com.tencentyun.TLSSigAPIv2;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "5.1 前台-直播模块")
@RestController
@RequestMapping("/frontend/live")
public class LiveController {

    // 读取配置
    @Value("${trtc.appid}")
    private long sdkAppId;

    @Value("${trtc.secret-key}")
    private String secretKey;

    @Value("${trtc.expire-time:86400}")
    private long expireTime;

    // 🔴 引入咱们刚刚建好的直播间大管家
    @Autowired
    private LiveRoomService liveRoomService;

    // 引入课时服务（为了查课时的名字）
    @Autowired
    private CourseHourService courseHourService;

    @Operation(summary = "进入直播间大礼包(获取房间号+门票)")
    // 🔴 接口地址改了，现在需要传入具体的课时ID
    @GetMapping("/enter/{hourId}")
    public Result<Map<String, Object>> enterLiveRoom(@PathVariable Long hourId) {
        
        // 1. 查出当前点击的这个课时叫啥名字
        CourseHour hour = courseHourService.getById(hourId);
        if (hour == null) {
            return Result.fail("课时不存在");
        }

        // 2. 召唤大管家：给这个课时分配/查询房间号！
        LiveRoom room = liveRoomService.getOrCreateRoom(hourId, hour.getName());

        // 3. 找腾讯云算门票 (UserSig)
        String userId = String.valueOf(StpUserUtil.getLoginIdAsLong());
        TLSSigAPIv2 api = new TLSSigAPIv2(sdkAppId, secretKey);
        String userSig = api.genUserSig(userId, expireTime);

        // 4. 把所有核心装备打包，一次性发给前端！
        Map<String, Object> result = new HashMap<>();
        result.put("sdkAppId", sdkAppId);
        result.put("userId", userId);
        result.put("userSig", userSig);
        result.put("roomId", room.getRoomNo()); // 🔴 新增：前端连接用的唯一房间号

        return Result.success(result);
    }
}