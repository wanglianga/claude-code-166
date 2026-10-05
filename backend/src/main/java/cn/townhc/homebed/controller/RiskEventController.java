package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.RiskEvent;
import cn.townhc.homebed.domain.enums.DomainEnums.RiskSeverity;
import cn.townhc.homebed.service.RiskEventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "风险事件")
@RestController
@RequestMapping("/api/risks")
@RequiredArgsConstructor
public class RiskEventController {

    private final RiskEventService riskEventService;

    @GetMapping
    public ApiResponse<List<RiskEvent>> list(@RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long patientId) {
        return ApiResponse.ok(riskEventService.list(status, patientId));
    }

    @PostMapping
    public ApiResponse<RiskEvent> report(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(riskEventService.manualReport(
                Long.valueOf(body.get("patientId").toString()),
                body.get("visitId") == null ? null : Long.valueOf(body.get("visitId").toString()),
                (String) body.getOrDefault("riskType", "其他风险"),
                body.get("severity") == null ? RiskSeverity.MEDIUM
                        : RiskSeverity.valueOf(body.get("severity").toString()),
                (String) body.getOrDefault("description", ""),
                (String) body.get("evidencePath")));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<RiskEvent> confirm(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(riskEventService.confirm(id, body.getOrDefault("handlePlan", "")));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<RiskEvent> close(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(riskEventService.close(id, body.getOrDefault("closeNote", "")));
    }
}
