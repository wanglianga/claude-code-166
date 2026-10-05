package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.MedicalOrder;
import cn.townhc.homebed.domain.enums.DomainEnums.OrderType;
import cn.townhc.homebed.service.MedicalOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "医嘱与远程复核")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class MedicalOrderController {

    private final MedicalOrderService orderService;

    @GetMapping
    public ApiResponse<List<MedicalOrder>> list(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) Long patientId,
                                                @RequestParam(required = false) Long visitId) {
        return ApiResponse.ok(orderService.list(status, patientId, visitId));
    }

    @Operation(summary = "医生依据巡诊记录下达医嘱（调处方/复诊/转诊/护理调整）")
    @PostMapping
    public ApiResponse<MedicalOrder> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.create(
                body.get("visitId") == null ? null : Long.valueOf(body.get("visitId").toString()),
                body.get("patientId") == null ? null : Long.valueOf(body.get("patientId").toString()),
                OrderType.valueOf(body.get("orderType").toString()),
                (String) body.get("content"),
                (String) body.get("diagnosisBasis"),
                (Boolean) body.get("coldChainRequired"),
                (String) body.get("coldChainNote")));
    }

    @Operation(summary = "另一名医生远程复核医嘱")
    @PostMapping("/{id}/remote-review")
    public ApiResponse<MedicalOrder> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.remoteReview(id,
                Boolean.TRUE.equals(body.get("approved")),
                (String) body.getOrDefault("comment", "")));
    }

    @PostMapping("/{id}/execute")
    public ApiResponse<MedicalOrder> execute(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.ok(orderService.execute(id, body == null ? "" : body.getOrDefault("note", "")));
    }

    @PostMapping("/{id}/void")
    public ApiResponse<MedicalOrder> voidOrder(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(orderService.voidOrder(id, body.getOrDefault("reason", "")));
    }
}
