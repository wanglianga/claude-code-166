package com.townclinic.homebed.service;

import com.townclinic.homebed.common.ApiException;
import com.townclinic.homebed.common.DomainEnums.ItemCategory;
import com.townclinic.homebed.common.DomainEnums.Role;
import com.townclinic.homebed.dto.CatalogItemReq;
import com.townclinic.homebed.dto.StockAdjustReq;
import com.townclinic.homebed.entity.CatalogItem;
import com.townclinic.homebed.repo.CatalogItemRepository;
import com.townclinic.homebed.security.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 医保/收费目录与耗材库存；目录变化会影响新发生项目，历史账单保留快照。 */
@Service
public class CatalogService {

    private final CatalogItemRepository repo;
    private final AuditService audit;

    public CatalogService(CatalogItemRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CatalogItem> list(String category) {
        Roles.requireLogin();
        if (category == null || category.isBlank()) return repo.findAll();
        return repo.findByCategoryAndEnabledTrue(ItemCategory.valueOf(category));
    }

    @Transactional
    public CatalogItem create(CatalogItemReq req) {
        Roles.require(Role.ADMIN, Role.INSURANCE);
        if (repo.existsByCode(req.code())) throw new ApiException("项目编码已存在：" + req.code());
        CatalogItem c = new CatalogItem();
        copy(c, req);
        c = repo.save(c);
        audit.log(0L, "CATALOG_ADD", "新增收费目录：" + c.getCode() + " " + c.getName());
        return c;
    }

    @Transactional
    public CatalogItem update(Long id, CatalogItemReq req) {
        Roles.require(Role.ADMIN, Role.INSURANCE);
        CatalogItem c = repo.findById(id).orElseThrow(() -> new ApiException("项目不存在"));
        copy(c, req);
        audit.log(0L, "CATALOG_CHANGE",
                "目录调整：" + c.getCode() + " " + c.getName()
                        + "；价格=" + c.getPrice() + "，报销比例=" + c.getReimburseRatio()
                        + (c.isInsuranceCovered() ? "（医保内）" : "（自费）"));
        return repo.save(c);
    }

    @Transactional
    public CatalogItem adjustStock(Long id, StockAdjustReq req) {
        Roles.require(Role.ADMIN, Role.NURSE);
        CatalogItem c = repo.findById(id).orElseThrow(() -> new ApiException("项目不存在"));
        int after = c.getStockQty() + (int) Math.round(req.changeQty().doubleValue());
        if (after < 0) throw new ApiException("调整后库存不能为负，当前库存 " + c.getStockQty());
        c.setStockQty(after);
        repo.save(c);
        audit.log(0L, "STOCK_ADJUST",
                (req.changeQty().doubleValue() >= 0 ? "耗材入库 " : "耗材盘亏/调整 ")
                        + c.getName() + " " + req.changeQty().stripTrailingZeros().toPlainString()
                        + c.getUnit() + "，结存 " + after + "；原因：" + req.reason());
        return c;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> lowStock() {
        Roles.require(Role.ADMIN, Role.NURSE);
        List<Map<String, Object>> out = new ArrayList<>();
        for (CatalogItem c : repo.findAll()) {
            if (c.getCategory() == ItemCategory.SERVICE) continue;
            if (c.getStockQty() <= c.getSafetyStock()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.getId());
                m.put("code", c.getCode());
                m.put("name", c.getName());
                m.put("category", c.getCategory());
                m.put("stockQty", c.getStockQty());
                m.put("safetyStock", c.getSafetyStock());
                m.put("coldChain", c.isColdChain());
                m.put("outOfStock", c.getStockQty() == 0);
                out.add(m);
            }
        }
        return out;
    }

    private void copy(CatalogItem c, CatalogItemReq req) {
        c.setCode(req.code());
        c.setName(req.name());
        c.setSpec(req.spec());
        c.setCategory(req.category());
        c.setPrice(req.price() == null ? BigDecimal.ZERO : req.price());
        c.setUnit(req.unit());
        c.setInsuranceCovered(req.insuranceCovered());
        c.setReimburseRatio(req.insuranceCovered() ? req.reimburseRatio() : 0);
        c.setColdChain(req.coldChain());
        if (req.stockQty() != null) c.setStockQty(req.stockQty());
        c.setSafetyStock(req.safetyStock() == null ? 0 : req.safetyStock());
        c.setDurationMin(req.durationMin() == null ? 30 : req.durationMin());
        c.setFrequencyLimit(req.frequencyLimit());
    }
}
