package com.vr.vr.domain;

import lombok.Data;

@Data
public class TenantStats {
    private Long tenantId;
    private Long applicationId;
    private Long count;
    private Double durationMinutes;
}
