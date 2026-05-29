package com.vr.vr.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppVersion {
    private Long id;
    private Long applicationId;
    private String version;
    private LocalDateTime createdAt;
}
