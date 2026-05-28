package com.vr.vr.engine;

import com.vr.vr.domain.Session;
import com.vr.vr.mapper.SessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class SessionTracker implements SessionLifecycle {
    private static final Logger log = LoggerFactory.getLogger(SessionTracker.class);

    private final SessionMapper sessionMapper;
    private final ReentrantLock lock = new ReentrantLock();
    private final int staleTimeoutSeconds;

    public SessionTracker(SessionMapper sessionMapper) {
        this(sessionMapper, 30);
    }

    public SessionTracker(SessionMapper sessionMapper, int staleTimeoutSeconds) {
        this.sessionMapper = sessionMapper;
        this.staleTimeoutSeconds = staleTimeoutSeconds;
    }

    @Override
    public Session startSession(Long venueId, Long appId, String version) {
        lock.lock();
        try {
            Session existing = sessionMapper.findActiveSession(venueId, appId);
            if (existing != null) {
                throw new IllegalStateException(
                    "active session already exists for venue=" + venueId + " app=" + appId);
            }
            Session session = new Session();
            session.setVenueId(venueId);
            session.setApplicationId(appId);
            session.setVersion(version != null ? version : "");
            session.setStartedAt(LocalDateTime.now());
            session.setStatus("active");
            sessionMapper.insertSession(session);
            log.info("session started: id={} venue={} app={}", session.getId(), venueId, appId);
            return session;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void endSession(Long sessionId) {
        sessionMapper.endSession(sessionId, "normal", LocalDateTime.now());
        log.info("session ended normally: id={}", sessionId);
    }

    @Override
    public List<Long> closeStaleSessions(Long venueId) {
        lock.lock();
        try {
            List<Long> closedIds = new ArrayList<>();
            List<Session> activeSessions;
            if (venueId != null && venueId > 0) {
                activeSessions = sessionMapper.selectActiveSessionsByVenueId(venueId);
            } else {
                activeSessions = sessionMapper.selectAllActiveSessions();
            }

            Instant threshold = Instant.now().minus(staleTimeoutSeconds, ChronoUnit.SECONDS);
            for (Session s : activeSessions) {
                if (s.getStartedAt() != null &&
                    s.getStartedAt().atZone(ZoneId.systemDefault()).toInstant().isBefore(threshold)) {
                    sessionMapper.endSession(s.getId(), "abnormal", LocalDateTime.now());
                    closedIds.add(s.getId());
                    log.info("session auto-closed (stale): id={} venue={}", s.getId(), s.getVenueId());
                }
            }
            return closedIds;
        } finally {
            lock.unlock();
        }
    }
}
