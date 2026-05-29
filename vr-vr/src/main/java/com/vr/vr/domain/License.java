package com.vr.vr.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class License {
    private Long id;
    private Long tenantId;
    private Long applicationId;
    private Boolean granted;
    private String quotaType;
    private Long quotaLimit;
    private Long quotaUsed;
    private LocalDateTime createdAt;
}
