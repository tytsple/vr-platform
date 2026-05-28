package com.vr.admin.controller.vr;

import com.vr.common.core.domain.AjaxResult;
import com.vr.framework.security.TokenService;
import com.vr.framework.security.context.LoginUser;
import com.vr.system.domain.SysOperLog;
import com.vr.system.domain.SysUser;
import com.vr.system.mapper.SysOperLogMapper;
import com.vr.system.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private SysUserService userService;
    @Autowired private TokenService tokenService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SysOperLogMapper operLogMapper;
    @Autowired private HttpServletRequest request;

    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
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
        logLogin(username, true, null);
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getUserName(), roles, tenantId);
        String token = tokenService.createToken(loginUser);
        return AjaxResult.success(Map.of("token", token));
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
