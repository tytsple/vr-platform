package com.vr.admin.controller.vr;

import com.vr.common.annotation.Log;
import com.vr.common.core.controller.BaseController;
import com.vr.common.core.domain.AjaxResult;
import com.vr.common.enums.BusinessType;
import com.vr.common.exception.ServiceException;
import com.vr.framework.security.PermissionService;
import com.vr.system.domain.SysUser;
import com.vr.system.mapper.SysUserMapper;
import com.vr.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class UserController extends BaseController {

    @Autowired private SysUserService userService;
    @Autowired private SysUserMapper userMapper;
    @Autowired private PermissionService permissionService;

    @GetMapping
    @PreAuthorize("@ss.hasRole('admin')")
    public List<SysUser> list() {
        return userService.selectUserList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult get(@PathVariable Long id) {
        SysUser user = userService.selectUserById(id);
        List<String> roles = userMapper.selectRoleKeysByUserId(id);
        Long tenantId = userMapper.selectTenantIdByUserId(id);
        return AjaxResult.success(Map.of(
            "user", user,
            "roles", roles != null ? roles : List.of(),
            "tenantId", tenantId
        ));
    }

    @Transactional
    @PostMapping
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    public AjaxResult create(@RequestBody Map<String, Object> body) {
        SysUser user = new SysUser();
        user.setUserName((String) body.get("userName"));
        user.setNickName((String) body.getOrDefault("nickName", ""));
        user.setPassword((String) body.get("password"));
        user.setStatus((String) body.getOrDefault("status", "0"));
        userService.insertUser(user);

        String roleKey = (String) body.get("roleKey");
        if (roleKey != null && !roleKey.isEmpty()) {
            userMapper.insertUserRole(user.getUserId(), roleKey);
        }
        Object tid = body.get("tenantId");
        if ("tenant".equals(roleKey)) {
            if (!(tid instanceof Number)) {
                return AjaxResult.error("租户用户必须选择所属租户");
            }
            userMapper.upsertUserTenant(user.getUserId(), ((Number) tid).longValue());
        } else if (body.containsKey("tenantId")) {
            if (tid instanceof Number) {
                userMapper.upsertUserTenant(user.getUserId(), ((Number) tid).longValue());
            } else {
                userMapper.deleteUserTenant(user.getUserId());
            }
        }
        return AjaxResult.success(user);
    }

    @Transactional
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public AjaxResult update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setUserName((String) body.get("userName"));
        user.setNickName((String) body.getOrDefault("nickName", ""));
        if (body.containsKey("password") && body.get("password") != null && !((String) body.get("password")).isEmpty()) {
            user.setPassword((String) body.get("password"));
        }
        user.setStatus((String) body.getOrDefault("status", "0"));
        userService.updateUser(user);

        String roleKey = (String) body.get("roleKey");
        if (roleKey != null && !roleKey.isEmpty()) {
            userMapper.deleteUserRoles(id);
            userMapper.insertUserRole(id, roleKey);
        }
        Object tid = body.get("tenantId");
        if ("tenant".equals(roleKey)) {
            if (!(tid instanceof Number)) {
                return AjaxResult.error("租户用户必须选择所属租户");
            }
            userMapper.upsertUserTenant(id, ((Number) tid).longValue());
        } else if (body.containsKey("tenantId")) {
            if (tid instanceof Number) {
                userMapper.upsertUserTenant(id, ((Number) tid).longValue());
            } else {
                userMapper.deleteUserTenant(id);
            }
        }
        return AjaxResult.success();
    }

    @Transactional
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (permissionService.getLoginUser() != null
            && permissionService.getLoginUser().getUserId().equals(id)) {
            throw new ServiceException(400, "不能删除自己的账号");
        }
        userMapper.deleteUserRoles(id);
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
