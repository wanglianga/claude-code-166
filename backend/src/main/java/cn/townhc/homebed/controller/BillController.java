package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.Bill;
import cn.townhc.homebed.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "统一账单与医保结算")
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillingService billingService;

    @GetMapping
    public ApiResponse<List<Bill>> list(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) Long patientId) {
        return ApiResponse.ok(billingService.list(status, patientId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(billingService.detail(id));
    }

    @Operation(summary = "财务按账期生成统一账单（护理+耗材+冷链，自动拆分医保/自费并合规初审）")
    @PostMapping("/generate")
    public ApiResponse<Bill> generate(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(billingService.generate(
                Long.valueOf(body.get("applicationId").toString()),
                LocalDate.parse(body.get("periodStart").toString()),
                LocalDate.parse(body.get("periodEnd").toString())));
    }

    @Operation(summary = "家属确认费用明细")
    @PostMapping("/{id}/confirm")
    public ApiResponse<Bill> confirm(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.ok(billingService.familyConfirm(id,
                body == null ? "家属确认无误" : body.getOrDefault("note", "家属确认无误")));
    }

    @Operation(summary = "家属质疑收费")
    @PostMapping("/{id}/dispute")
    public ApiResponse<Bill> dispute(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(billingService.dispute(id, body.getOrDefault("note", "")));
    }

    @Operation(summary = "财务答复收费质疑（可剔除争议明细后重算）")
    @PostMapping("/{id}/resolve-dispute")
    public ApiResponse<Bill> resolveDispute(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(billingService.resolveDispute(id,
                (String) body.getOrDefault("resolution", ""),
                Boolean.TRUE.equals(body.get("adjust"))));
    }

    @PostMapping("/{id}/items/{itemId}/remove")
    public ApiResponse<Bill> removeItem(@PathVariable Long id, @PathVariable Long itemId,
                                        @RequestBody Map<String, String> body) {
        return ApiResponse.ok(billingService.removeItem(id, itemId,
                body.getOrDefault("reason", "异议成立")));
    }

    @Operation(summary = "医保复核完成后结算")
    @PostMapping("/{id}/settle")
    public ApiResponse<Bill> settle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(billingService.settle(id,
                body.getOrDefault("insuranceSettleNo", "YB" + System.currentTimeMillis())));
    }
}
