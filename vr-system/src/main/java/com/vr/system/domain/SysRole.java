package com.vr.system.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysRole {
    private Long roleId;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String status;
    private LocalDateTime createTime;
}
