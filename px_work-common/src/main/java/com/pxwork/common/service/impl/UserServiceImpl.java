package com.pxwork.common.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.common.entity.Department;
import com.pxwork.common.entity.User;
import com.pxwork.common.entity.UserDepartment;
import com.pxwork.common.mapper.UserMapper;
import com.pxwork.common.request.FrontendLoginRequest;
import com.pxwork.common.service.DepartmentService;
import com.pxwork.common.service.UserDepartmentService;
import com.pxwork.common.service.UserService;
import com.pxwork.common.utils.StpUserUtil;

import cn.dev33.satoken.secure.SaSecureUtil;

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
        if (StringUtils.isNotBlank(user.getPassword())) {
            user.setPassword(SaSecureUtil.sha256(user.getPassword()));
        }
        boolean saved = this.save(user);
        if (!saved) {
            return false;
        }
        userDepartmentService.remove(new LambdaQueryWrapper<UserDepartment>()
                .eq(UserDepartment::getUserId, user.getId()));
        saveDepartment(user.getId(), user.getDepartmentId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        boolean updated = this.updateById(user);
        if (!updated) {
            return false;
        }
        userDepartmentService.remove(new LambdaQueryWrapper<UserDepartment>()
                .eq(UserDepartment::getUserId, user.getId()));
        saveDepartment(user.getId(), user.getDepartmentId());
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
        
        List<Long> userIds = userPage.getRecords().stream().map(User::getId).collect(Collectors.toList());
        List<UserDepartment> userDepartments = userDepartmentService.list(new LambdaQueryWrapper<UserDepartment>()
                .in(UserDepartment::getUserId, userIds));

        Map<Long, Long> userDeptIdMap = userDepartments.stream()
                .collect(Collectors.toMap(UserDepartment::getUserId, UserDepartment::getDepartmentId, (first, second) -> first));
        List<Long> deptIds = userDeptIdMap.values().stream().distinct().collect(Collectors.toList());
        Map<Long, Department> deptMap = deptIds.isEmpty() ? Map.of() : departmentService.listByIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, dept -> dept));

        for (User user : userPage.getRecords()) {
            Long departmentId = userDeptIdMap.get(user.getId());
            user.setDepartmentId(departmentId);
            user.setDepartment(departmentId == null ? null : deptMap.get(departmentId));
        }
        
        return userPage;
    }

    @Override
    public Map<String, Object> login(FrontendLoginRequest request) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getIdCard, request.getIdCard()));
        if (user == null) {
            throw new RuntimeException("账号或密码错误");
        }
        String password = SaSecureUtil.sha256(request.getPassword());
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("账号或密码错误");
        }
        StpUserUtil.login(user.getId());
        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("token", StpUserUtil.getTokenValue());
        loginInfo.put("isFirstLogin", user.getIsFirstLogin());
        return loginInfo;
    }

    private void saveDepartment(Long userId, Long departmentId) {
        if (departmentId != null) {
            UserDepartment userDepartment = new UserDepartment();
            userDepartment.setUserId(userId);
            userDepartment.setDepartmentId(departmentId);
            userDepartmentService.save(userDepartment);
        }
    }
}
