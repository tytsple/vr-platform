package com.vr.admin.controller.vr;

import com.vr.common.annotation.Log;
import com.vr.common.core.controller.BaseController;
import com.vr.common.enums.BusinessType;
import com.vr.vr.domain.License;
import com.vr.vr.mapper.LicenseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/licenses")
public class LicenseController extends BaseController {

    @Autowired private LicenseMapper licenseMapper;

    @GetMapping
    @PreAuthorize("@ss.hasRole('admin')")
    public List<License> list(@RequestParam(required = false) Long tenant_id) {
        List<License> list = licenseMapper.selectLicenseList(
            tenant_id != null ? tenant_id : 0);
        return list != null ? list : List.of();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    public License get(@PathVariable Long id) {
        return licenseMapper.selectLicenseById(id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "授权管理", businessType = BusinessType.INSERT)
    public ResponseEntity<License> create(@RequestBody License license) {
        if (license.getGranted() == null) license.setGranted(true);
        if (license.getQuotaType() == null) license.setQuotaType("");
        if (license.getQuotaUsed() == null) license.setQuotaUsed(0L);
        licenseMapper.insertLicense(license);
        return ResponseEntity.status(201).body(license);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "授权管理", businessType = BusinessType.UPDATE)
    public License update(@PathVariable Long id, @RequestBody License license) {
        license.setId(id);
        licenseMapper.updateLicense(license);
        return license;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "授权管理", businessType = BusinessType.DELETE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        licenseMapper.deleteLicenseById(id);
        return ResponseEntity.noContent().build();
    }
}
