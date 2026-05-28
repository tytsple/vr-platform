package com.vr.vr.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class License {
    private Long id;
    @JsonProperty("tenant_id")
    private Long tenantId;
    @JsonProperty("application_id")
    private Long applicationId;
    private Boolean granted;
    @JsonProperty("quota_type")
    private String quotaType;
    @JsonProperty("quota_limit")
    private Long quotaLimit;
    @JsonProperty("quota_used")
    private Long quotaUsed;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
