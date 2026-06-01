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
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long venueId : activeIds) {
            Venue venue = venueMapper.selectVenueById(venueId);
            Map<String, Object> status = new HashMap<>();
            status.put("venueId", venueId);
            status.put("name", venue != null ? venue.getName() : "未知");
            status.put("address", venue != null ? venue.getAddress() : "");
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
