package com.vr.system.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long userId;
    private String userName;
    private String nickName;
    @JsonIgnore
    private String password;
    private String status;
    private String delFlag;
    private Integer tokenVersion;
    private LocalDateTime createTime;
}
