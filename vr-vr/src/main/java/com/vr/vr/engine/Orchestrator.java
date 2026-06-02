package com.vr.vr.engine;

import com.vr.vr.mapper.VenueMapper;
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
    private final VenueMapper venueMapper;

    public Orchestrator(MessageRouter router, SessionLifecycle sessions,
                        LicenseValidator licenses, VenueMapper venueMapper) {
        this.router = router;
        this.sessions = sessions;
        this.licenses = licenses;
        this.venueMapper = venueMapper;
    }

    @PostConstruct
    public void start() {
        router.onMessage("session_start", msg -> {
            try {
                Long tenantId = venueMapper.selectTenantIdByVenueId(msg.getVenueId());
                if (tenantId != null && !licenses.checkLicense(tenantId, msg.getAppId())) {
                    log.warn("license denied: venue={} app={}", msg.getVenueId(), msg.getAppId());
                    router.sendToVenue(msg.getVenueId(), java.util.Map.of(
                        "type", "license_denied",
                        "app_id", msg.getAppId(),
                        "reason", "未授权或配额已用完"
                    ));
                    return;
                }
                sessions.startSession(msg.getVenueId(), msg.getAppId(), msg.getVersion());
            } catch (IllegalStateException e) {
                log.warn("duplicate session_start: {}", e.getMessage());
            }
        });

        router.onMessage("session_end", msg -> {
            if (msg.getSessionId() != null && msg.getSessionId() > 0) {
                if (!sessions.isSessionOwnedBy(msg.getSessionId(), msg.getVenueId())) {
                    log.warn("session_end venue mismatch: session={} venue={}",
                        msg.getSessionId(), msg.getVenueId());
                    return;
                }
                sessions.endSession(msg.getSessionId());
                licenses.consumeQuota(msg.getVenueId(), msg.getAppId());
            }
        });
    }

    @Scheduled(fixedDelay = 15000)
    public void sweepStaleSessions() {
        List<Long> active = router.getActiveVenues();
        List<Long> closed = sessions.closeStaleSessions(active);
        if (!closed.isEmpty()) {
            log.info("sweep: auto-closed {} stale sessions", closed.size());
        }
    }
}
