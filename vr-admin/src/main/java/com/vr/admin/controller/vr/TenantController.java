package com.vr.admin.controller.vr;

import com.vr.common.annotation.Log;
import com.vr.common.core.controller.BaseController;
import com.vr.common.enums.BusinessType;
import com.vr.common.exception.ServiceException;
import com.vr.vr.domain.Tenant;
import com.vr.vr.mapper.LicenseMapper;
import com.vr.vr.mapper.TenantMapper;
import com.vr.vr.mapper.VenueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
public class TenantController extends BaseController {

    @Autowired private TenantMapper tenantMapper;
    @Autowired private VenueMapper venueMapper;
    @Autowired private LicenseMapper licenseMapper;

    @GetMapping
    @PreAuthorize("@ss.hasRole('admin')")
    public List<Tenant> list() {
        return tenantMapper.selectTenantList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    public Tenant get(@PathVariable Long id) {
        return tenantMapper.selectTenantById(id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "租户管理", businessType = BusinessType.INSERT)
    public ResponseEntity<Tenant> create(@RequestBody Tenant tenant) {
        tenantMapper.insertTenant(tenant);
        return ResponseEntity.status(201).body(tenant);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    public Tenant update(@PathVariable Long id, @RequestBody Tenant tenant) {
        tenant.setId(id);
        tenantMapper.updateTenant(tenant);
        return tenant;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "租户管理", businessType = BusinessType.DELETE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        int venueCount = venueMapper.selectVenueList(id).size();
        if (venueCount > 0) {
            throw new ServiceException(409,
                "该租户下有 " + venueCount + " 个场地，请先删除所有场地再删除租户");
        }
        int licenseCount = licenseMapper.selectLicenseList(id).size();
        if (licenseCount > 0) {
            throw new ServiceException(409,
                "该租户下有 " + licenseCount + " 个授权记录，请先删除授权再删除租户");
        }
        tenantMapper.deleteTenantById(id);
        return ResponseEntity.noContent().build();
    }
}
