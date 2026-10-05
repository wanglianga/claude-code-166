package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.ChargeCatalog;
import cn.townhc.homebed.domain.enums.DomainEnums.CatalogCategory;
import cn.townhc.homebed.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "收费与医保目录（含目录变更版本）")
@RestController
@RequestMapping("/api/catalogs")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ApiResponse<List<ChargeCatalog>> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ApiResponse.ok(catalogService.list(includeInactive));
    }

    @GetMapping("/categories")
    public ApiResponse<List<Map<String, String>>> categories() {
        return ApiResponse.ok(catalogService.categories().stream()
                .map(c -> Map.of("name", c.name(), "label", c.label)).toList());
    }

    @PostMapping
    public ApiResponse<ChargeCatalog> create(@RequestBody ChargeCatalog catalog) {
        return ApiResponse.ok(catalogService.create(catalog));
    }

    @Operation(summary = "医保目录变化：旧版本失效，按新版本生效（历史账单保留快照）")
    @PostMapping("/{id}/revise")
    public ApiResponse<ChargeCatalog> revise(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ChargeCatalog input = new ChargeCatalog();
        if (body.get("name") != null) input.setName((String) body.get("name"));
        if (body.get("category") != null) input.setCategory(CatalogCategory.valueOf((String) body.get("category")));
        if (body.get("unit") != null) input.setUnit((String) body.get("unit"));
        if (body.get("unitPrice") != null) {
            input.setUnitPrice(new java.math.BigDecimal(body.get("unitPrice").toString()));
        }
        if (body.get("reimbursable") != null) input.setReimbursable(Boolean.valueOf(body.get("reimbursable").toString()));
        if (body.get("insuranceCategory") != null) input.setInsuranceCategory((String) body.get("insuranceCategory"));
        if (body.get("reimbursementRate") != null) {
            input.setReimbursementRate(new java.math.BigDecimal(body.get("reimbursementRate").toString()));
        }
        if (body.get("coldChain") != null) input.setColdChain(Boolean.valueOf(body.get("coldChain").toString()));
        if (body.get("limitRule") != null) input.setLimitRule((String) body.get("limitRule"));
        if (body.get("dailyLimit") != null) input.setDailyLimit(Integer.valueOf(body.get("dailyLimit").toString()));
        return ApiResponse.ok(catalogService.revise(id, input,
                (String) body.getOrDefault("versionNote", "医保目录调整")));
    }

    @PostMapping("/{id}/toggle")
    public ApiResponse<ChargeCatalog> toggle(@PathVariable Long id, @RequestParam boolean active) {
        return ApiResponse.ok(catalogService.toggleActive(id, active));
    }
}
