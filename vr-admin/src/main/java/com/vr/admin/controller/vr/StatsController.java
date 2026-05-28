package com.vr.admin.controller.vr;

import com.vr.common.core.controller.BaseController;
import com.vr.vr.domain.Session;
import com.vr.vr.domain.TenantStats;
import com.vr.vr.mapper.SessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class StatsController extends BaseController {

    @Autowired private SessionMapper sessionMapper;

    @GetMapping("/stats")
    @PreAuthorize("@ss.hasRole('admin')")
    public List<TenantStats> stats(
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) Long venue_id) {
        List<TenantStats> list = sessionMapper.selectSessionStats();
        return list != null ? list : List.of();
    }

    @GetMapping("/sessions")
    @PreAuthorize("@ss.hasRole('admin')")
    public List<Session> sessions(
            @RequestParam(required = false) Long venue_id,
            @RequestParam(required = false) Long app_id,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        List<Session> list = sessionMapper.selectSessionList(
            venue_id, app_id, from, to, status, limit);
        return list != null ? list : List.of();
    }
}
