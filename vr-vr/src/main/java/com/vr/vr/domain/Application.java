package com.vr.vr.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Application {
    private Long id;
    private String name;
    private String description;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
