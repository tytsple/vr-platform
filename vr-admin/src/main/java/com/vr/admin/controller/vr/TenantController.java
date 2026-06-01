package com.vr.admin.controller.vr;

import com.vr.common.annotation.Log;
import com.vr.common.core.controller.BaseController;
import com.vr.common.enums.BusinessType;
import com.vr.vr.domain.Tenant;
import com.vr.vr.mapper.TenantMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.vr.common.core.page.TableDataInfo;
import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
public class TenantController extends BaseController {

    @Autowired private TenantMapper tenantMapper;

    @GetMapping
    @PreAuthorize("@ss.hasRole('admin')")
    public TableDataInfo list() {
        startPage();
        List<Tenant> list = tenantMapper.selectTenantList();
        return getDataTable(list);
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
        tenantMapper.deleteTenantById(id);
        return ResponseEntity.noContent().build();
    }
}
