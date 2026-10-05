package com.townclinic.homebed.controller;

import com.townclinic.homebed.common.R;
import com.townclinic.homebed.dto.GeneratePlanReq;
import com.townclinic.homebed.dto.PlanCancelReq;
import com.townclinic.homebed.service.SchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "巡诊排班与上门路线")
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final SchedulingService service;

    public PlanController(SchedulingService service) { this.service = service; }

    @PostMapping("/generate")
    @Operation(summary = "按日期区间生成巡诊计划并自动排班（含可解释说明）")
    public R<?> generate(@Valid @RequestBody GeneratePlanReq req) {
        return R.ok(service.generate(req));
    }

    @PostMapping("/assign")
    @Operation(summary = "对指定日期重新自动排班")
    public R<?> assign(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(service.assignDate(date));
    }

    @GetMapping("/board")
    @Operation(summary = "某日上门路线看板（护士负荷/半径/挂起任务）")
    public R<?> board(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(service.dayBoard(date));
    }

    @PostMapping("/{id}/reassign")
    @Operation(summary = "人工改派护士")
    public R<?> reassign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long nurseId = Long.valueOf(body.get("nurseId").toString());
        String note = body.get("note") == null ? null : body.get("note").toString();
        return R.ok(service.reassign(id, nurseId, note));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消/终止单次上门（家属不在/缺货等）")
    public R<?> cancel(@PathVariable Long id, @RequestBody(required = false) PlanCancelReq req) {
        return R.ok(service.cancelPlan(id, req));
    }
}
