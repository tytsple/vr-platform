package com.vr.admin.controller.vr;

import com.vr.common.core.controller.BaseController;
import com.vr.vr.domain.SessionVO;
import com.vr.vr.domain.Venue;
import com.vr.vr.engine.MessageRouter;
import com.vr.vr.mapper.SessionMapper;
import com.vr.vr.mapper.VenueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/operator")
public class OperatorController extends BaseController {

    @Autowired private SessionMapper sessionMapper;
    @Autowired private MessageRouter messageRouter;
    @Autowired private VenueMapper venueMapper;

    @GetMapping("/venues/status")
    @PreAuthorize("@ss.hasRole('operator')")
    public List<Map<String, Object>> venuesStatus() {
        List<Long> activeIds = messageRouter.getActiveVenues();
        if (activeIds.isEmpty()) return List.of();
        List<Venue> venues = venueMapper.selectVenuesByIds(activeIds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Venue venue : venues) {
            Map<String, Object> status = new HashMap<>();
            status.put("venueId", venue.getId());
            status.put("name", venue.getName());
            status.put("address", venue.getAddress() != null ? venue.getAddress() : "");
            status.put("online", true);
            result.add(status);
        }
        return result;
    }

    @GetMapping("/sessions/active")
    @PreAuthorize("@ss.hasRole('operator')")
    public List<SessionVO> activeSessions() {
        return sessionMapper.selectActiveSessionVOList(100);
    }
}
