package com.vr.vr.engine;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Orchestrator {
    private static final Logger log = LoggerFactory.getLogger(Orchestrator.class);

    private final MessageRouter router;
    private final SessionLifecycle sessions;
    private final LicenseValidator licenses;

    public Orchestrator(MessageRouter router, SessionLifecycle sessions, LicenseValidator licenses) {
        this.router = router;
        this.sessions = sessions;
        this.licenses = licenses;
    }

    @PostConstruct
    public void start() {
        router.onMessage("session_start", msg -> {
            try {
                sessions.startSession(msg.getVenueId(), msg.getAppId(), msg.getVersion());
            } catch (IllegalStateException e) {
                log.warn("duplicate session_start: {}", e.getMessage());
            }
        });

        router.onMessage("session_end", msg -> {
            if (msg.getSessionId() != null && msg.getSessionId() > 0) {
                sessions.endSession(msg.getSessionId());
                licenses.consumeQuota(msg.getVenueId(), msg.getAppId());
            }
        });
    }

    @Scheduled(fixedDelay = 15000)
    public void sweepStaleSessions() {
        List<Long> closed = sessions.closeStaleSessions(null);
        if (!closed.isEmpty()) {
            log.info("sweep: auto-closed {} stale sessions", closed.size());
        }
    }
}
