package com.vr.system.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long userId;
    private String userName;
    private String nickName;
    private String password;
    private String status;
    private String delFlag;
    private LocalDateTime createTime;
}
