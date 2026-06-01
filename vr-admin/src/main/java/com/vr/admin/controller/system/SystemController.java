package com.vr.admin.controller.system;

import com.vr.common.core.domain.AjaxResult;
import com.vr.framework.security.PermissionService;
import com.vr.framework.security.context.LoginUser;
import com.vr.system.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SystemController {

    @GetMapping({"/health", "/api/health"})
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    @RequestMapping("/api")

    @Autowired private PermissionService permissionService;
    @Autowired private SysMenuService menuService;

    /** Get current user info (used by frontend on every page load) */
    @GetMapping("/getInfo")
    public AjaxResult getInfo() {
        LoginUser user = permissionService.getLoginUser();
        if (user == null) return AjaxResult.error(401, "not logged in");
        return AjaxResult.success(Map.of(
            "user", Map.of(
                "userId", user.getUserId(),
                "userName", user.getUsername(),
                "roles", user.getRoles(),
                "permissions", List.of("*:*:*")
            ),
            "roles", user.getRoles(),
            "permissions", List.of("*:*:*")
        ));
    }

    /** Get router menu tree for current user (dynamic routing) */
    @GetMapping("/getRouters")
    public AjaxResult getRouters() {
        LoginUser user = permissionService.getLoginUser();
        if (user == null) return AjaxResult.error(401, "not logged in");
        List<Map<String, Object>> routers = menuService.buildRouterTree(user.getUserId());
        return AjaxResult.success(routers);
    }
}
