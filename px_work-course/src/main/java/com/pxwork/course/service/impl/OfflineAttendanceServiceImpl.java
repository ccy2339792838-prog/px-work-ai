package com.pxwork.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.OfflineAttendance;
import com.pxwork.course.mapper.OfflineAttendanceMapper;
import com.pxwork.course.service.OfflineAttendanceService;
import org.springframework.stereotype.Service;

@Service
public class OfflineAttendanceServiceImpl extends ServiceImpl<OfflineAttendanceMapper, OfflineAttendance> implements OfflineAttendanceService {
}
