package com.vr.vr.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Session {
    private Long id;
    @JsonProperty("venue_id")
    private Long venueId;
    @JsonProperty("application_id")
    private Long applicationId;
    private String version;
    @JsonProperty("started_at")
    private LocalDateTime startedAt;
    @JsonProperty("ended_at")
    private LocalDateTime endedAt;
    private String status;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
