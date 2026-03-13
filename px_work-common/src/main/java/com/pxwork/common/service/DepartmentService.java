package com.pxwork.common.service;

import com.pxwork.common.entity.Department;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 部门表 服务类
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
public interface DepartmentService extends IService<Department> {

    List<Department> getTree();
}
