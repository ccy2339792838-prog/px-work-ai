package com.pxwork.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.common.entity.Department;
import com.pxwork.common.entity.User;
import com.pxwork.common.entity.UserDepartment;
import com.pxwork.common.mapper.UserMapper;
import com.pxwork.common.service.DepartmentService;
import com.pxwork.common.service.UserDepartmentService;
import com.pxwork.common.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 学员用户表 服务实现类
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserDepartmentService userDepartmentService;
    
    @Autowired
    private DepartmentService departmentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createUser(User user) {
        // 1. 保存学员基本信息
        boolean saved = this.save(user);
        if (!saved) {
            return false;
        }

        // 2. 保存部门关联
        saveDepartments(user.getId(), user.getDepartmentIds());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        // 1. 更新学员基本信息
        boolean updated = this.updateById(user);
        if (!updated) {
            return false;
        }

        // 2. 更新部门关联（如果 departmentIds 不为 null）
        if (user.getDepartmentIds() != null) {
            // 先删除旧关联
            userDepartmentService.remove(new LambdaQueryWrapper<UserDepartment>()
                    .eq(UserDepartment::getUserId, user.getId()));
            // 再保存新关联
            saveDepartments(user.getId(), user.getDepartmentIds());
        }
        return true;
    }
    
    @Override
    public Page<User> pageWithDepts(Page<User> page, String name) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(User::getName, name);
        }
        queryWrapper.orderByDesc(User::getCreatedAt);
        
        Page<User> userPage = this.page(page, queryWrapper);
        
        if (userPage.getRecords().isEmpty()) {
            return userPage;
        }
        
        // 批量查询部门信息
        List<Long> userIds = userPage.getRecords().stream().map(User::getId).collect(Collectors.toList());
        List<UserDepartment> userDepartments = userDepartmentService.list(new LambdaQueryWrapper<UserDepartment>()
                .in(UserDepartment::getUserId, userIds));
                
        if (userDepartments.isEmpty()) {
            return userPage;
        }
        
        List<Long> allDeptIds = userDepartments.stream().map(UserDepartment::getDepartmentId).distinct().collect(Collectors.toList());
        if (allDeptIds.isEmpty()) {
             return userPage;
        }
        
        Map<Long, Department> deptMap = departmentService.listByIds(allDeptIds).stream()
                .collect(Collectors.toMap(Department::getId, dept -> dept));
        
        // 组装数据
        Map<Long, List<Department>> userDeptMap = userDepartments.stream()
            .filter(ud -> deptMap.containsKey(ud.getDepartmentId()))
            .collect(Collectors.groupingBy(
                UserDepartment::getUserId,
                Collectors.mapping(ud -> deptMap.get(ud.getDepartmentId()), Collectors.toList())
            ));
            
        for (User user : userPage.getRecords()) {
            user.setDepartments(userDeptMap.get(user.getId()));
        }
        
        return userPage;
    }

    private void saveDepartments(Long userId, List<Long> departmentIds) {
        if (departmentIds != null && !departmentIds.isEmpty()) {
            List<UserDepartment> userDepartments = departmentIds.stream().map(deptId -> {
                UserDepartment ud = new UserDepartment();
                ud.setUserId(userId);
                ud.setDepartmentId(deptId);
                return ud;
            }).collect(Collectors.toList());
            userDepartmentService.saveBatch(userDepartments);
        }
    }
}
