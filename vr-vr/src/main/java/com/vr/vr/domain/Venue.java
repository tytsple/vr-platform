package com.vr.vr.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Venue {
    private Long id;
    private Long tenantId;
    private String name;
    private String address;
    private String controllerToken;
    private LocalDateTime createdAt;
}
