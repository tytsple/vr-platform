package com.vr.admin.controller.vr;

import com.vr.common.core.domain.AjaxResult;
import com.vr.framework.security.PermissionService;
import com.vr.framework.security.TokenService;
import com.vr.framework.security.context.LoginUser;
import com.vr.system.domain.SysOperLog;
import com.vr.system.domain.SysUser;
import com.vr.system.mapper.SysOperLogMapper;
import com.vr.system.mapper.SysUserMapper;
import com.vr.system.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private SysUserService userService;
    @Autowired private TokenService tokenService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PermissionService permissionService;
    @Autowired private SysOperLogMapper operLogMapper;
    @Autowired private SysUserMapper sysUserMapper;
    @Autowired private HttpServletRequest request;

    private final ConcurrentHashMap<String, long[]> loginAttempts = new ConcurrentHashMap<>();

    private boolean isRateLimited(String key) {
        long now = System.currentTimeMillis();
        long[] entry = loginAttempts.computeIfAbsent(key, k -> new long[]{0, now});
        synchronized (entry) {
            if (now - entry[1] > 60000) { entry[0] = 1; entry[1] = now; return false; }
            entry[0]++;
            if (entry[0] > 5) return true;
            return false;
        }
    }

    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String clientIp = request.getRemoteAddr();

        if (isRateLimited(clientIp)) {
            return AjaxResult.error(429, "请求过于频繁，请稍后再试");
        }
        SysUser user = userService.selectUserByUserName(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            logLogin(username, false, "bad credentials");
            return AjaxResult.error(401, "用户名或密码错误");
        }
        if (!"0".equals(user.getStatus())) {
            logLogin(username, false, "account disabled");
            return AjaxResult.error(403, "账户已被禁用");
        }
        List<String> roles = userService.selectRoleKeysByUserId(user.getUserId());
        if (roles == null || roles.isEmpty()) {
            logLogin(username, false, "no role assigned");
            return AjaxResult.error(403, "账户未分配角色，请联系管理员");
        }
        Long tenantId = userService.selectTenantIdByUserId(user.getUserId());
        sysUserMapper.incrementTokenVersion(user.getUserId());
        Integer version = sysUserMapper.selectTokenVersion(user.getUserId());
        logLogin(username, true, null);
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getUserName(), roles, tenantId);
        String token = tokenService.createToken(loginUser, version != null ? version : 1);
        return AjaxResult.success(Map.of("token", token));
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult changePassword(@RequestBody Map<String, String> body) {
        LoginUser loginUser = permissionService.getLoginUser();
        if (loginUser == null) return AjaxResult.error(401, "未登录");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null || newPassword.length() < 6) {
            return AjaxResult.error("新密码至少6位");
        }
        SysUser user = userService.selectUserById(loginUser.getUserId());
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            return AjaxResult.error("原密码错误");
        }
        SysUser update = new SysUser();
        update.setUserId(user.getUserId());
        update.setPassword(newPassword);
        userService.updateUser(update);
        return AjaxResult.success();
    }

    private void logLogin(String username, boolean success, String errorMsg) {
        SysOperLog log = new SysOperLog();
        log.setTitle("登录");
        log.setOperName(username);
        log.setRequestMethod("POST");
        log.setOperUrl("/api/auth/login");
        log.setOperIp(request.getRemoteAddr());
        log.setStatus(success ? 0 : 1);
        log.setErrorMsg(errorMsg);
        log.setCostTime(0L);
        log.setBusinessType("OTHER");
        log.setOperatorType("OTHER");
        operLogMapper.insertOperLog(log);
    }
}
