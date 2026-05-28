package com.vr.vr.engine;

import com.vr.vr.domain.License;
import com.vr.vr.domain.Venue;
import com.vr.vr.mapper.LicenseMapper;
import com.vr.vr.mapper.VenueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LicenseEngine implements LicenseValidator {
    private static final Logger log = LoggerFactory.getLogger(LicenseEngine.class);

    private final LicenseMapper licenseMapper;
    private final VenueMapper venueMapper;
    private final MessageRouter router;

    public LicenseEngine(LicenseMapper licenseMapper, VenueMapper venueMapper, MessageRouter router) {
        this.licenseMapper = licenseMapper;
        this.venueMapper = venueMapper;
        this.router = router;
    }

    @Override
    public boolean checkLicense(Long tenantId, Long appId) {
        License license = licenseMapper.selectLicenseByTenantAndApp(tenantId, appId);
        if (license == null || !Boolean.TRUE.equals(license.getGranted())) return false;
        String qt = license.getQuotaType();
        if (qt == null || qt.isEmpty()) return true;
        return license.getQuotaUsed() < license.getQuotaLimit();
    }

    @Override
    public void consumeQuota(Long venueId, Long appId) {
        Long tenantId = venueMapper.selectTenantIdByVenueId(venueId);
        License license = licenseMapper.selectLicenseByTenantAndApp(tenantId, appId);
        if (license == null || !Boolean.TRUE.equals(license.getGranted())) return;
        String qt = license.getQuotaType();
        if (qt == null || qt.isEmpty()) return;

        int affected = licenseMapper.incrementQuotaUsed(license.getId());
        if (affected > 0) {
            long remaining = license.getQuotaLimit() - (license.getQuotaUsed() + 1);
            if (remaining <= 0) {
                log.info("quota exhausted: tenant={} app={}", tenantId, appId);
                router.sendToVenue(venueId, Map.of(
                    "type", "quota_update",
                    "app_id", appId,
                    "quota_remaining", 0
                ));
            }
        }
    }

    @Override
    public long getQuotaRemaining(Long tenantId, Long appId) {
        License license = licenseMapper.selectLicenseByTenantAndApp(tenantId, appId);
        if (license == null || !Boolean.TRUE.equals(license.getGranted())) return 0;
        String qt = license.getQuotaType();
        if (qt == null || qt.isEmpty()) return -1;
        return license.getQuotaLimit() - license.getQuotaUsed();
    }

    @Override
    public void pushLicenseUpdate(Long tenantId, Long appId, boolean granted) {
        List<Venue> venues = venueMapper.selectVenueList(tenantId);
        for (Venue v : venues) {
            router.sendToVenue(v.getId(), Map.of(
                "type", "license_update",
                "app_id", appId,
                "granted", granted
            ));
        }
    }
}
