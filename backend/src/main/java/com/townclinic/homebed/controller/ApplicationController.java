package com.townclinic.homebed.controller;

import com.townclinic.homebed.common.R;
import com.townclinic.homebed.dto.AssessReq;
import com.townclinic.homebed.dto.PauseReq;
import com.townclinic.homebed.dto.SubmitApplicationReq;
import com.townclinic.homebed.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "家庭病床申请/评估/建床")
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "家属提交家庭病床申请")
    public R<?> submit(@Valid @RequestBody SubmitApplicationReq req) {
        return R.ok(service.submit(req));
    }

    @PostMapping("/assess")
    @Operation(summary = "家庭医生评估（病情/距离/照护人/医保/护理项目/风险）")
    public R<?> assess(@Valid @RequestBody AssessReq req) {
        return R.ok(service.assess(req));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过并建床")
    public R<?> approve(@PathVariable Long id) {
        return R.ok(service.approve(id));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "暂停病床（临时住院/家属不在家等）")
    public R<?> pause(@PathVariable Long id, @RequestBody(required = false) PauseReq req) {
        return R.ok(service.pause(id, req == null ? null : req.reason()));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复巡诊")
    public R<?> resume(@PathVariable Long id, @RequestBody(required = false) PauseReq req) {
        return R.ok(service.resume(id, req == null ? null : req.reason()));
    }

    @PostMapping("/{id}/discharge")
    @Operation(summary = "撤床结案")
    public R<?> discharge(@PathVariable Long id, @RequestBody(required = false) PauseReq req) {
        return R.ok(service.discharge(id, req == null ? null : req.reason()));
    }

    @GetMapping("/mine")
    @Operation(summary = "家属查看我的申请")
    public R<?> mine() { return R.ok(service.listMine()); }

    @GetMapping
    @Operation(summary = "申请列表（可按状态过滤）")
    public R<?> list(@RequestParam(required = false) String status) {
        return R.ok(service.listAll(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "申请详情（含患者/计划/巡诊/账单/时间线）")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(service.detail(id));
    }
}
