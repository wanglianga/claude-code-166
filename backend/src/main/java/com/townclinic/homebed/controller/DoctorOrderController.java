package com.townclinic.homebed.controller;

import com.townclinic.homebed.common.R;
import com.townclinic.homebed.dto.OrderReq;
import com.townclinic.homebed.service.DoctorOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "医生医嘱（调处方/复诊/转诊）")
@RestController
@RequestMapping("/api/orders")
public class DoctorOrderController {

    private final DoctorOrderService service;

    public DoctorOrderController(DoctorOrderService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "依据巡诊记录下达医嘱")
    public R<?> create(@Valid @RequestBody OrderReq req) {
        return R.ok(service.create(req));
    }

    @PostMapping("/{id}/exec")
    @Operation(summary = "更新医嘱执行情况")
    public R<?> exec(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(service.updateExec(id, body.get("status"), body.get("note")));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "某家庭病床的全部医嘱")
    public R<?> list(@PathVariable Long applicationId) {
        return R.ok(service.listByApplication(applicationId));
    }
}
