package com.vr.admin.controller.vr;

import com.vr.common.annotation.Log;
import com.vr.common.core.controller.BaseController;
import com.vr.common.enums.BusinessType;
import com.vr.vr.domain.Venue;
import com.vr.vr.mapper.VenueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/venues")
public class VenueController extends BaseController {

    @Autowired private VenueMapper venueMapper;

    private final SecureRandom rng = new SecureRandom();

    private String generateToken() {
        byte[] b = new byte[32];
        rng.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    @GetMapping
    @PreAuthorize("@ss.hasRole('admin')")
    public List<Venue> list(@RequestParam(required = false) Long tenant_id) {
        return venueMapper.selectVenueList(tenant_id != null ? tenant_id : 0);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    public Venue get(@PathVariable Long id) {
        return venueMapper.selectVenueById(id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "场地管理", businessType = BusinessType.INSERT)
    public ResponseEntity<Venue> create(@RequestBody Venue venue) {
        venue.setControllerToken(generateToken());
        venueMapper.insertVenue(venue);
        return ResponseEntity.status(201).body(venue);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "场地管理", businessType = BusinessType.UPDATE)
    public Venue update(@PathVariable Long id, @RequestBody Venue venue) {
        venue.setId(id);
        venueMapper.updateVenue(venue);
        return venue;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "场地管理", businessType = BusinessType.DELETE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        venueMapper.deleteVenueById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerate-token")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "场地管理", businessType = BusinessType.UPDATE)
    public Map<String, String> regenerateToken(@PathVariable Long id) {
        String token = generateToken();
        venueMapper.updateControllerToken(id, token);
        return Map.of("controller_token", token);
    }
}
