package com.vr.vr.engine;

import com.vr.vr.domain.Session;

import java.util.List;

/** Manages VR session lifecycle: start, end, stale sweep. */
public interface SessionLifecycle {
    Session startSession(Long venueId, Long appId, String version);
    void endSession(Long sessionId);
    List<Long> closeStaleSessions(Long venueId);
}
