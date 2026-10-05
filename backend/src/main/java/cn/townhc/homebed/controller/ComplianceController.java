package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.ComplianceRecord;
import cn.townhc.homebed.domain.enums.DomainEnums.ComplianceStatus;
import cn.townhc.homebed.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "医保合规审查")
@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceService complianceService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(complianceService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(complianceService.detail(id));
    }

    @GetMapping("/bill/{billId}")
    public ApiResponse<ComplianceRecord> byBill(@PathVariable Long billId) {
        return ApiResponse.ok(complianceService.byBill(billId));
    }

    @Operation(summary = "重新执行自动初审")
    @PostMapping("/bill/{billId}/rerun")
    public ApiResponse<ComplianceRecord> rerun(@PathVariable Long billId) {
        return ApiResponse.ok(complianceService.rerunByBill(billId));
    }

    @Operation(summary = "医保经办人员复核结论")
    @PostMapping("/{id}/audit")
    public ApiResponse<ComplianceRecord> audit(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(complianceService.audit(id,
                ComplianceStatus.valueOf(body.getOrDefault("result", "COMPLIANT")),
                body.getOrDefault("comment", ""),
                body.getOrDefault("conclusion", ""),
                body.getOrDefault("agencyName", "镇医保服务站")));
    }
}
