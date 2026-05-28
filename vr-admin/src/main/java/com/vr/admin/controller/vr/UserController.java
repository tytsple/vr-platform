package com.vr.admin.controller.vr;

import com.vr.common.annotation.Log;
import com.vr.common.core.controller.BaseController;
import com.vr.common.core.domain.AjaxResult;
import com.vr.common.enums.BusinessType;
import com.vr.system.domain.SysUser;
import com.vr.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserController extends BaseController {

    @Autowired private SysUserService userService;

    @GetMapping
    @PreAuthorize("@ss.hasRole('admin')")
    public List<SysUser> list() {
        List<SysUser> list = userService.selectUserList();
        return list != null ? list : List.of();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    public SysUser get(@PathVariable Long id) {
        return userService.selectUserById(id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    public ResponseEntity<SysUser> create(@RequestBody SysUser user) {
        userService.insertUser(user);
        return ResponseEntity.status(201).body(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public AjaxResult update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setUserId(id);
        userService.updateUser(user);
        return AjaxResult.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
