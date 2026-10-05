package com.townclinic.homebed.controller;

import com.townclinic.homebed.common.R;
import com.townclinic.homebed.dto.HandleRiskReq;
import com.townclinic.homebed.dto.RiskEventReq;
import com.townclinic.homebed.service.RiskEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "风险事件")
@RestController
@RequestMapping("/api/risks")
public class RiskController {

    private final RiskEventService service;

    public RiskController(RiskEventService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "上报风险事件（病情恶化/压疮/超时/冷链/投诉）")
    public R<?> report(@Valid @RequestBody RiskEventReq req) {
        return R.ok(service.report(req));
    }

    @PostMapping("/handle")
    @Operation(summary = "管理者/医生处置风险事件")
    public R<?> handle(@Valid @RequestBody HandleRiskReq req) {
        return R.ok(service.handle(req));
    }

    @GetMapping
    @Operation(summary = "风险事件列表（可按状态过滤）")
    public R<?> list(@RequestParam(required = false) String status) {
        return R.ok(service.list(status));
    }
}
