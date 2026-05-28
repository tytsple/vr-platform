package com.vr.system.service;

import com.vr.system.domain.SysMenu;
import com.vr.system.mapper.SysMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SysMenuService {

    @Autowired
    private SysMenuMapper menuMapper;

    public List<SysMenu> selectMenuTreeByUserId(Long userId) {
        List<SysMenu> all = menuMapper.selectMenuTreeByUserId(userId);
        return buildTree(all, 0L);
    }

    private List<SysMenu> buildTree(List<SysMenu> all, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu m : all) {
            if (m.getParentId().equals(parentId)) {
                m.setChildren(buildTree(all, m.getMenuId()));
                tree.add(m);
            }
        }
        return tree;
    }

    /** Convert menu tree to Vue Router format */
    public List<Map<String, Object>> buildRouterTree(Long userId) {
        List<SysMenu> menus = selectMenuTreeByUserId(userId);
        return convertToRouter(menus);
    }

    private List<Map<String, Object>> convertToRouter(List<SysMenu> menus) {
        List<Map<String, Object>> routers = new ArrayList<>();
        for (SysMenu m : menus) {
            Map<String, Object> router = new LinkedHashMap<>();
            router.put("name", m.getMenuName());
            router.put("path", m.getPath());
            router.put("component", m.getComponent());
            router.put("hidden", "1".equals(m.getVisible()));
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("title", m.getMenuName());
            meta.put("icon", m.getIcon());
            router.put("meta", meta);
            if (m.getChildren() != null && !m.getChildren().isEmpty()) {
                router.put("children", convertToRouter(m.getChildren()));
            }
            routers.add(router);
        }
        return routers;
    }
}
