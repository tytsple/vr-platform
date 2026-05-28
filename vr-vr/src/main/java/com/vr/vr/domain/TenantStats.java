package com.vr.vr.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TenantStats {
    @JsonProperty("tenant_id")
    private Long tenantId;
    @JsonProperty("application_id")
    private Long applicationId;
    private Long count;
    @JsonProperty("duration_minutes")
    private Double durationMinutes;
}
