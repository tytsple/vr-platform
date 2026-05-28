package com.vr.system.domain;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysMenu {
    private Long menuId;
    private String menuName;
    private Long parentId;
    private Integer orderNum;
    private String path;
    private String component;
    private String menuType;
    private String visible;
    private String status;
    private String perms;
    private String icon;
    private LocalDateTime createTime;
    private List<SysMenu> children;
}
