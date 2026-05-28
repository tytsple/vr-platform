package com.vr.admin.controller.vr;

import com.vr.common.core.controller.BaseController;
import com.vr.vr.domain.Session;
import com.vr.vr.mapper.SessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/operator")
public class OperatorController extends BaseController {

    @Autowired private SessionMapper sessionMapper;

    @GetMapping("/venues/status")
    @PreAuthorize("@ss.hasRole('operator')")
    public List<Map<String, Object>> venuesStatus() {
        return List.of();
    }

    @GetMapping("/sessions/active")
    @PreAuthorize("@ss.hasRole('operator')")
    public List<Session> activeSessions() {
        List<Session> list = sessionMapper.selectAllActiveSessions();
        return list != null ? list : List.of();
    }
}
