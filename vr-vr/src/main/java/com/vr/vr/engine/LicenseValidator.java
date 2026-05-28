package com.vr.vr.engine;

/** Checks licenses and consumes quota for a tenant+app pair. */
public interface LicenseValidator {
    boolean checkLicense(Long tenantId, Long appId);
    void consumeQuota(Long venueId, Long appId);
    long getQuotaRemaining(Long tenantId, Long appId);
    void pushLicenseUpdate(Long tenantId, Long appId, boolean granted);
}
