package com.pxwork.course.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.CourseCategory;
import com.pxwork.course.mapper.CourseCategoryMapper;
import com.pxwork.course.service.CourseCategoryService;

@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory> implements CourseCategoryService {

    @Override
    public List<CourseCategory> listTree(String industry) {
        LambdaQueryWrapper<CourseCategory> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(industry)) {
            queryWrapper.eq(CourseCategory::getIndustry, industry);
        }
        queryWrapper.orderByAsc(CourseCategory::getSort).orderByAsc(CourseCategory::getId);
        List<CourseCategory> allCategories = list(queryWrapper);
        return buildTree(allCategories, 0L);
    }

    private List<CourseCategory> buildTree(List<CourseCategory> categories, Long parentId) {
        List<CourseCategory> tree = new ArrayList<>();
        for (CourseCategory category : categories) {
            if (category.getParentId().equals(parentId)) {
                category.setChildren(buildTree(categories, category.getId()));
                tree.add(category);
            }
        }
        return tree;
    }
}
