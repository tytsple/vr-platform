package com.vr.system.mapper;

import com.vr.system.domain.SysMenu;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysMenuMapper {
    @Select("SELECT m.menu_id, m.menu_name, m.parent_id, m.order_num, m.path, m.component, m.menu_type, m.visible, m.status, m.perms, m.icon, m.create_time FROM sys_menu m WHERE m.status='0' AND m.visible='0' ORDER BY m.parent_id, m.order_num")
    List<SysMenu> selectMenuTreeAll();

    @Select("SELECT DISTINCT m.menu_id, m.menu_name, m.parent_id, m.order_num, m.path, m.component, m.menu_type, m.visible, m.status, m.perms, m.icon, m.create_time FROM sys_menu m LEFT JOIN sys_role_menu rm ON m.menu_id=rm.menu_id LEFT JOIN sys_user_role ur ON rm.role_id=ur.role_id WHERE m.status='0' AND m.visible='0' AND ur.user_id=#{userId} ORDER BY m.parent_id, m.order_num")
    List<SysMenu> selectMenuTreeByUserId(Long userId);
}
