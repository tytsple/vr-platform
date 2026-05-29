package com.vr.vr.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Session {
    private Long id;
    private Long venueId;
    private Long applicationId;
    private String version;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String status;
    private LocalDateTime createdAt;
}
