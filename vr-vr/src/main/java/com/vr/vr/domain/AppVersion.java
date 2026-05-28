package com.vr.vr.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppVersion {
    private Long id;
    @JsonProperty("application_id")
    private Long applicationId;
    private String version;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
