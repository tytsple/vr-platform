package com.vr.vr.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class Message {
    private String type;
    @JsonProperty("venue_id")
    private Long venueId;
    @JsonProperty("session_id")
    private Long sessionId;
    @JsonProperty("app_id")
    private Long appId;
    private String version;
    private Long timestamp;
    private JsonNode payload;
}
