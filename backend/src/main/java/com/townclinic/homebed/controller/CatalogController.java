package com.townclinic.homebed.controller;

import com.townclinic.homebed.common.R;
import com.townclinic.homebed.dto.CatalogItemReq;
import com.townclinic.homebed.dto.StockAdjustReq;
import com.townclinic.homebed.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "医保收费目录与耗材库存")
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "目录列表（可按分类过滤）")
    public R<?> list(@RequestParam(required = false) String category) {
        return R.ok(service.list(category));
    }

    @PostMapping
    @Operation(summary = "新增目录项目（管理者/医保经办）")
    public R<?> create(@Valid @RequestBody CatalogItemReq req) {
        return R.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "调整目录项目（价格/报销比例/医保内外，历史账单保留快照）")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody CatalogItemReq req) {
        return R.ok(service.update(id, req));
    }

    @PostMapping("/{id}/stock")
    @Operation(summary = "耗材入库/盘亏调整")
    public R<?> stock(@PathVariable Long id, @Valid @RequestBody StockAdjustReq req) {
        return R.ok(service.adjustStock(id, req));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "低库存/缺货预警（可能导致上门耗材缺货）")
    public R<?> low() { return R.ok(service.lowStock()); }
}
