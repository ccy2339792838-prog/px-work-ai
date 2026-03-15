package com.pxwork.api.controller.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pxwork.common.dto.UserExcelDTO;
import com.pxwork.common.entity.User;
import com.pxwork.common.service.UserService;
import com.pxwork.common.utils.Result;
import com.pxwork.system.entity.SysDict;
import com.pxwork.system.service.SysDictService;

import cn.dev33.satoken.secure.SaSecureUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * 学员管理 前端控制器
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Tag(name = "1.5 后台-学员信息管理")
@RestController
@RequestMapping("/backend/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private SysDictService sysDictService;

    @Operation(summary = "学员分页列表", description = "获取学员分页列表(单部门信息)")
    @GetMapping("/list")
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        
        Page<User> page = new Page<>(current, size);
        return Result.success(userService.pageWithDepts(page, name));
    }

    @Operation(summary = "新增学员", description = "创建新学员(仅支持绑定一个部门)")
    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody User user) {
        boolean success = userService.createUser(user);
        return success ? Result.success(true) : Result.fail("创建失败");
    }

    @Operation(summary = "修改学员", description = "更新学员信息(仅支持绑定一个部门)")
    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody User user) {
        boolean success = userService.updateUser(user);
        return success ? Result.success(true) : Result.fail("更新失败");
    }

    @Operation(summary = "删除学员", description = "根据ID删除学员")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = userService.removeById(id);
        return success ? Result.success(true) : Result.fail("删除失败");
    }

    @Operation(summary = "批量导入学员")
    @PostMapping("/import")
    public Result<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("文件不能为空");
        }
        List<UserExcelDTO> rows;
        try {
            rows = EasyExcel.read(file.getInputStream()).head(UserExcelDTO.class).sheet().doReadSync();
        } catch (IOException e) {
            return Result.fail("Excel 解析失败");
        }
        if (rows == null || rows.isEmpty()) {
            return Result.fail("Excel 数据为空");
        }
        List<SysDict> dicts = sysDictService.list(new LambdaQueryWrapper<SysDict>()
                .in(SysDict::getDictType, List.of("job_role", "industry")));
        Map<String, String> jobRoleDict = dicts.stream()
                .filter(item -> "job_role".equals(item.getDictType()))
                .collect(Collectors.toMap(SysDict::getDictLabel, SysDict::getDictValue, (a, b) -> a));
        Map<String, String> industryDict = dicts.stream()
                .filter(item -> "industry".equals(item.getDictType()))
                .collect(Collectors.toMap(SysDict::getDictLabel, SysDict::getDictValue, (a, b) -> a));

        Map<String, User> importUserMap = new HashMap<>();
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            UserExcelDTO row = rows.get(i);
            int lineNo = i + 2;
            if (row == null) {
                errors.add("第" + lineNo + "行为空");
                continue;
            }
            if (!org.springframework.util.StringUtils.hasText(row.getIdCard())
                    || !org.springframework.util.StringUtils.hasText(row.getEnterprise())
                    || !org.springframework.util.StringUtils.hasText(row.getJobRole())
                    || !org.springframework.util.StringUtils.hasText(row.getIndustry())) {
                errors.add("第" + lineNo + "行存在必填项为空");
                continue;
            }
            String jobRoleValue = jobRoleDict.get(row.getJobRole());
            String industryValue = industryDict.get(row.getIndustry());
            if (jobRoleValue == null || industryValue == null) {
                errors.add("第" + lineNo + "行存在未配置的岗位/行业");
                continue;
            }
            User user = new User();
            user.setName(row.getName());
            user.setIdCard(row.getIdCard());
            user.setEnterprise(row.getEnterprise());
            user.setJobRole(jobRoleValue);
            user.setIndustry(industryValue);
            String idCard = row.getIdCard();
            String rawPassword = idCard.length() > 6 ? idCard.substring(idCard.length() - 6) : idCard;
            user.setPassword(SaSecureUtil.sha256(rawPassword));
            user.setIsFirstLogin(1);
            importUserMap.put(row.getIdCard(), user);
        }
        if (!errors.isEmpty()) {
            return Result.fail("存在未配置的岗位/行业或必填项缺失: " + String.join("；", errors));
        }

        List<String> idCards = new ArrayList<>(importUserMap.keySet());
        List<User> exists = userService.list(new LambdaQueryWrapper<User>().in(User::getIdCard, idCards));
        Map<String, User> existMap = exists.stream().collect(Collectors.toMap(User::getIdCard, item -> item));

        List<User> inserts = new ArrayList<>();
        List<User> updates = new ArrayList<>();
        for (User importUser : importUserMap.values()) {
            User exist = existMap.get(importUser.getIdCard());
            if (exist == null) {
                inserts.add(importUser);
            } else {
                importUser.setId(exist.getId());
                importUser.setPassword(exist.getPassword());
                importUser.setIsFirstLogin(exist.getIsFirstLogin());
                updates.add(importUser);
            }
        }
        if (!inserts.isEmpty()) {
            userService.saveBatch(inserts);
        }
        if (!updates.isEmpty()) {
            userService.updateBatchById(updates);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("total", rows.size());
        data.put("inserted", inserts.size());
        data.put("updated", updates.size());
        return Result.success(data);
    }
}
