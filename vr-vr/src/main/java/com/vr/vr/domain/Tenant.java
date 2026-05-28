package com.vr.vr.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Tenant {
    private Long id;
    private String name;
    @JsonProperty("contact_info")
    private String contactInfo;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
