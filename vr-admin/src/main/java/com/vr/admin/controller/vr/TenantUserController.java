package com.vr.admin.controller.vr;

import com.vr.common.core.controller.BaseController;
import com.vr.framework.security.TenantContext;
import com.vr.vr.domain.License;
import com.vr.vr.domain.Session;
import com.vr.vr.domain.TenantStats;
import com.vr.vr.domain.Venue;
import com.vr.vr.mapper.LicenseMapper;
import com.vr.vr.mapper.SessionMapper;
import com.vr.vr.mapper.VenueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tenant")
public class TenantUserController extends BaseController {

    @Autowired private VenueMapper venueMapper;
    @Autowired private LicenseMapper licenseMapper;
    @Autowired private SessionMapper sessionMapper;

    @GetMapping("/venues")
    @PreAuthorize("@ss.hasRole('tenant')")
    public List<Venue> venues() {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return List.of();
        List<Venue> list = venueMapper.selectVenueList(tenantId);
        return list != null ? list : List.of();
    }

    @GetMapping("/licenses")
    @PreAuthorize("@ss.hasRole('tenant')")
    public List<License> licenses() {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return List.of();
        List<License> list = licenseMapper.selectLicenseList(tenantId);
        return list != null ? list : List.of();
    }

    @GetMapping("/stats")
    @PreAuthorize("@ss.hasRole('tenant')")
    public List<TenantStats> stats(
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId != null) {
            return sessionMapper.selectSessionStatsByTenantId(tenantId, from, to);
        }
        return List.of();
    }

    @GetMapping("/sessions")
    @PreAuthorize("@ss.hasRole('tenant')")
    public List<Session> sessions() {
        Long tenantId = TenantContext.getCurrentTenantId();
        List<Venue> venues = venueMapper.selectVenueList(tenantId);
        List<Long> venueIds = venues.stream().map(Venue::getId).toList();
        if (venueIds.isEmpty()) {
            return List.of();
        }
        return sessionMapper.selectSessionsByVenueIds(venueIds, 100);
    }
}
