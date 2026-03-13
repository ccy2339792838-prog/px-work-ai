package com.pxwork.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.common.entity.Department;
import com.pxwork.common.mapper.DepartmentMapper;
import com.pxwork.common.service.DepartmentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 部门表 服务实现类
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Override
    public List<Department> getTree() {
        // 1. 获取所有部门
        List<Department> allDepts = this.list(new LambdaQueryWrapper<Department>()
                .orderByAsc(Department::getSort));

        if (allDepts == null || allDepts.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 组装树形结构
        // 先找到所有顶级节点 (parentId = 0)
        List<Department> roots = allDepts.stream()
                .filter(dept -> dept.getParentId() == 0)
                .collect(Collectors.toList());

        // 递归查找子节点
        for (Department root : roots) {
            buildChildren(root, allDepts);
        }

        return roots;
    }

    private void buildChildren(Department parent, List<Department> allDepts) {
        List<Department> children = allDepts.stream()
                .filter(dept -> dept.getParentId().equals(parent.getId()))
                .collect(Collectors.toList());
        
        if (!children.isEmpty()) {
            parent.setChildren(children);
            for (Department child : children) {
                buildChildren(child, allDepts);
            }
        }
    }
}
