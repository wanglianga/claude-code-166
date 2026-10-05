package cn.townhc.homebed.service;

import cn.townhc.homebed.common.BusinessException;
import cn.townhc.homebed.domain.entity.ChargeCatalog;
import cn.townhc.homebed.domain.entity.Material;
import cn.townhc.homebed.domain.entity.MaterialTransaction;
import cn.townhc.homebed.domain.enums.DomainEnums.MaterialTxType;
import cn.townhc.homebed.repository.ChargeCatalogRepository;
import cn.townhc.homebed.repository.MaterialRepository;
import cn.townhc.homebed.repository.MaterialTransactionRepository;
import cn.townhc.homebed.security.LoginUser;
import cn.townhc.homebed.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialTransactionRepository transactionRepository;
    private final ChargeCatalogRepository catalogRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Material> list(Boolean lowStockOnly) {
        if (Boolean.TRUE.equals(lowStockOnly)) {
            return materialRepository.findByStockQtyLessThanEqualOrderByStockQtyAsc(0);
        }
        return materialRepository.findByActiveTrueOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<Material> shortageWarning() {
        // 低于安全库存（含 0 缺货）
        return materialRepository.findByActiveTrueOrderByCodeAsc().stream()
                .filter(m -> m.getStockQty() != null && m.getSafetyStock() != null
                        && m.getStockQty() <= m.getSafetyStock())
                .toList();
    }

    @Transactional
    public Material saveMaterial(Material input) {
        Material m;
        if (input.getId() != null) {
            m = materialRepository.findById(input.getId()).orElseThrow();
        } else {
            m = new Material();
            m.setCode(input.getCode());
        }
        m.setName(input.getName());
        m.setSpec(input.getSpec());
        m.setUnit(input.getUnit());
        m.setCategory(input.getCategory());
        m.setColdChain(input.getColdChain());
        m.setSafetyStock(input.getSafetyStock() == null ? 0 : input.getSafetyStock());
        m.setUnitCost(input.getUnitCost());
        m.setNearestExpiry(input.getNearestExpiry());
        if (input.getCatalog() != null && input.getCatalog().getId() != null) {
            ChargeCatalog c = catalogRepository.findById(input.getCatalog().getId()).orElse(null);
            m.setCatalog(c);
        }
        if (input.getStockQty() != null) m.setStockQty(input.getStockQty());
        return materialRepository.save(m);
    }

    public record StockInCommand(Integer quantity, String batchNo, LocalDate expiryDate,
                                 Double coldChainTemp, String note) {}

    /** 入库（冷链药品登记入库温度） */
    @Transactional
    public MaterialTransaction stockIn(Long materialId, StockInCommand cmd) {
        LoginUser me = SecurityUtils.current();
        Material m = materialRepository.findById(materialId)
                .orElseThrow(() -> new BusinessException(404, "耗材不存在"));
        int qty = cmd.quantity() == null ? 0 : cmd.quantity();
        if (qty <= 0) throw new BusinessException("入库数量必须大于 0");
        m.setStockQty((m.getStockQty() == null ? 0 : m.getStockQty()) + qty);
        if (cmd.expiryDate() != null) m.setNearestExpiry(cmd.expiryDate());
        materialRepository.save(m);

        MaterialTransaction tx = new MaterialTransaction();
        tx.setMaterial(m);
        tx.setType(MaterialTxType.IN);
        tx.setQuantity(qty);
        tx.setBatchNo(cmd.batchNo());
        tx.setExpiryDate(cmd.expiryDate());
        tx.setColdChainTemp(cmd.coldChainTemp());
        tx.setNote(cmd.note());
        tx.setOccurredAt(LocalDateTime.now());
        tx = transactionRepository.save(tx);
        auditService.log(me, "耗材", "入库", m.getId(), m.getCode(), null,
                m.getName() + " 入库 " + qty + m.getUnit()
                        + (m.getColdChain() && cmd.coldChainTemp() != null
                        ? "，冷链温度 " + cmd.coldChainTemp() + "℃" : ""));
        return tx;
    }

    @Transactional(readOnly = true)
    public List<MaterialTransaction> transactions(String materialId) {
        if (materialId != null && !materialId.isBlank()) {
            return transactionRepository.findByMaterialIdOrderByOccurredAtDesc(Long.valueOf(materialId));
        }
        return transactionRepository.findAll().stream()
                .sorted((a, b) -> b.getOccurredAt().compareTo(a.getOccurredAt())).toList();
    }
}
