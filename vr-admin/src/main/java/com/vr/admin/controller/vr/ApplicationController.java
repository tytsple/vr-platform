package com.vr.admin.controller.vr;

import com.vr.common.annotation.Log;
import com.vr.common.core.controller.BaseController;
import com.vr.common.enums.BusinessType;
import com.vr.vr.domain.AppVersion;
import com.vr.vr.domain.Application;
import com.vr.vr.mapper.ApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.vr.common.core.page.TableDataInfo;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/applications")
public class ApplicationController extends BaseController {

    @Autowired private ApplicationMapper appMapper;

    @GetMapping
    @PreAuthorize("@ss.hasRole('admin')")
    public TableDataInfo list() {
        startPage();
        List<Application> list = appMapper.selectApplicationList();
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    public Application get(@PathVariable Long id) {
        return appMapper.selectApplicationById(id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "应用管理", businessType = BusinessType.INSERT)
    public ResponseEntity<Application> create(@RequestBody Application app) {
        appMapper.insertApplication(app);
        return ResponseEntity.status(201).body(app);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "应用管理", businessType = BusinessType.UPDATE)
    public Application update(@PathVariable Long id, @RequestBody Application app) {
        app.setId(id);
        appMapper.updateApplication(app);
        return app;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "应用管理", businessType = BusinessType.DELETE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        appMapper.deleteApplicationById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("@ss.hasRole('admin')")
    public List<AppVersion> listVersions(@PathVariable Long id) {
        return appMapper.selectVersionsByAppId(id);
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("@ss.hasRole('admin')")
    public AppVersion createVersion(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AppVersion v = new AppVersion();
        v.setApplicationId(id);
        v.setVersion(body.get("version"));
        appMapper.insertVersion(v);
        return v;
    }

    @DeleteMapping("/{id}/versions/{vid}")
    @PreAuthorize("@ss.hasRole('admin')")
    public ResponseEntity<Void> deleteVersion(@PathVariable Long id, @PathVariable Long vid) {
        appMapper.deleteVersionById(vid);
        return ResponseEntity.noContent().build();
    }
}
