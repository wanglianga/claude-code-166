package com.townclinic.homebed.controller;

import com.townclinic.homebed.common.R;
import com.townclinic.homebed.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理看板统计")
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) { this.service = service; }

    @GetMapping("/dashboard")
    @Operation(summary = "巡诊负荷/上门半径/耗材消耗/风险事件/结算概况")
    public R<?> dashboard() {
        return R.ok(service.dashboard());
    }
}
