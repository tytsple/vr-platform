package com.vr.system.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysOperLog {
    private Long operId;
    private String title;
    private String businessType;
    private String method;
    private String requestMethod;
    private String operatorType;
    private String operName;
    private String operUrl;
    private String operIp;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime operTime;
}
