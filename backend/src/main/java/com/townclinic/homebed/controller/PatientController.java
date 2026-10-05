package com.townclinic.homebed.controller;

import com.townclinic.homebed.common.R;
import com.townclinic.homebed.dto.PatientReq;
import com.townclinic.homebed.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "患者档案")
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final ApplicationService service;

    public PatientController(ApplicationService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "患者列表")
    public R<?> list() { return R.ok(service.listPatients()); }

    @GetMapping("/{id}")
    @Operation(summary = "患者详情")
    public R<?> get(@PathVariable Long id) { return R.ok(service.getPatient(id)); }

    @PostMapping
    @Operation(summary = "新建患者档案")
    public R<?> create(@Valid @RequestBody PatientReq req) { return R.ok(service.createPatient(req)); }

    @PutMapping("/{id}")
    @Operation(summary = "修改患者档案")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody PatientReq req) {
        return R.ok(service.updatePatient(id, req));
    }
}
