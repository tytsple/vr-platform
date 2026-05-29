package com.vr.vr.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Tenant {
    private Long id;
    private String name;
    private String contactInfo;
    private LocalDateTime createdAt;
}
