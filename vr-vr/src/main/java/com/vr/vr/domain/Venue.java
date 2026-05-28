package com.vr.vr.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Venue {
    private Long id;
    @JsonProperty("tenant_id")
    private Long tenantId;
    private String name;
    private String address;
    @JsonProperty("controller_token")
    private String controllerToken;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
