package cn.townhc.homebed.controller;

import cn.townhc.homebed.common.ApiResponse;
import cn.townhc.homebed.domain.entity.Material;
import cn.townhc.homebed.domain.entity.MaterialTransaction;
import cn.townhc.homebed.service.MaterialService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "耗材与冷链药品库存")
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ApiResponse<List<Material>> list(@RequestParam(required = false) Boolean lowStockOnly) {
        return ApiResponse.ok(materialService.list(lowStockOnly));
    }

    @GetMapping("/shortage")
    public ApiResponse<List<Material>> shortage() {
        return ApiResponse.ok(materialService.shortageWarning());
    }

    @PostMapping
    public ApiResponse<Material> save(@RequestBody Material material) {
        return ApiResponse.ok(materialService.saveMaterial(material));
    }

    @PostMapping("/{id}/stock-in")
    public ApiResponse<MaterialTransaction> stockIn(@PathVariable Long id,
                                                    @RequestBody MaterialService.StockInCommand cmd) {
        return ApiResponse.ok(materialService.stockIn(id, cmd));
    }

    @GetMapping("/transactions")
    public ApiResponse<List<MaterialTransaction>> transactions(@RequestParam(required = false) String materialId) {
        return ApiResponse.ok(materialService.transactions(materialId));
    }
}
