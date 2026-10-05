package cn.townhc.homebed.service;

import cn.townhc.homebed.common.BusinessException;
import cn.townhc.homebed.domain.entity.ChargeCatalog;
import cn.townhc.homebed.domain.enums.DomainEnums.CatalogCategory;
import cn.townhc.homebed.repository.ChargeCatalogRepository;
import cn.townhc.homebed.security.LoginUser;
import cn.townhc.homebed.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 收费/医保目录管理。医保目录变化时：旧版本置失效（effectiveTo 封顶、active=false），
 * 新增生效版本；历史账单保留当时快照不受影响。
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ChargeCatalogRepository catalogRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ChargeCatalog> list(boolean includeInactive) {
        return includeInactive
                ? catalogRepository.findAllByOrderByCategoryAscCodeAsc()
                : catalogRepository.findByActiveTrueOrderByCategoryAscCodeAsc();
    }

    @Transactional
    public ChargeCatalog create(ChargeCatalog input) {
        LoginUser me = SecurityUtils.current();
        if (input.getCode() == null || input.getCode().isBlank()) {
            throw new BusinessException("目录编码必填");
        }
        catalogRepository.findAllByOrderByCategoryAscCodeAsc().stream()
                .filter(c -> c.getCode().equals(input.getCode()) && Boolean.TRUE.equals(c.getActive()))
                .findFirst().ifPresent(c -> {
                    throw new BusinessException("已存在生效中的同编码目录，请用“目录变更”发新版本");
                });
        input.setId(null);
        input.setActive(true);
        if (input.getEffectiveFrom() == null) input.setEffectiveFrom(LocalDate.now());
        ChargeCatalog saved = catalogRepository.save(input);
        auditService.log(me, "收费目录", "新增目录项", saved.getId(), saved.getCode(), null,
                saved.getName() + " 单价 " + saved.getUnitPrice()
                        + " 报销=" + (Boolean.TRUE.equals(saved.getReimbursable())
                        ? saved.getInsuranceCategory() + "/" + saved.getReimbursementRate() : "自费"));
        return saved;
    }

    /**
     * 目录变更：旧版本生效截止到昨天并失效，新版本今天生效。
     */
    @Transactional
    public ChargeCatalog revise(Long oldId, ChargeCatalog input, String versionNote) {
        LoginUser me = SecurityUtils.current();
        ChargeCatalog old = catalogRepository.findById(oldId)
                .orElseThrow(() -> new BusinessException(404, "原目录项不存在"));
        LocalDate today = LocalDate.now();
        old.setActive(false);
        old.setEffectiveTo(today.minusDays(1));
        catalogRepository.save(old);

        ChargeCatalog fresh = new ChargeCatalog();
        fresh.setCode(old.getCode());
        fresh.setName(input.getName() != null ? input.getName() : old.getName());
        fresh.setCategory(input.getCategory() != null ? input.getCategory() : old.getCategory());
        fresh.setUnit(input.getUnit() != null ? input.getUnit() : old.getUnit());
        fresh.setUnitPrice(input.getUnitPrice() != null ? input.getUnitPrice() : old.getUnitPrice());
        fresh.setReimbursable(input.getReimbursable() != null ? input.getReimbursable() : old.getReimbursable());
        fresh.setInsuranceCategory(input.getInsuranceCategory() != null
                ? input.getInsuranceCategory() : old.getInsuranceCategory());
        fresh.setReimbursementRate(input.getReimbursementRate() != null
                ? input.getReimbursementRate() : old.getReimbursementRate());
        fresh.setColdChain(input.getColdChain() != null ? input.getColdChain() : old.getColdChain());
        fresh.setLimitRule(input.getLimitRule() != null ? input.getLimitRule() : old.getLimitRule());
        fresh.setDailyLimit(input.getDailyLimit() != null ? input.getDailyLimit() : old.getDailyLimit());
        fresh.setActive(true);
        fresh.setEffectiveFrom(today);
        fresh.setVersionNote(versionNote);
        ChargeCatalog saved = catalogRepository.save(fresh);
        auditService.log(me, "收费目录", "医保目录变更发版", saved.getId(), saved.getCode(), null,
                "旧版本(id=" + oldId + ")失效；新版本：单价 " + saved.getUnitPrice()
                        + "，可报=" + saved.getReimbursable()
                        + "，比例=" + saved.getReimbursementRate()
                        + "；说明：" + versionNote);
        return saved;
    }

    @Transactional
    public ChargeCatalog toggleActive(Long id, boolean active) {
        LoginUser me = SecurityUtils.current();
        ChargeCatalog c = catalogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "目录项不存在"));
        c.setActive(active);
        if (!active) c.setEffectiveTo(LocalDate.now());
        catalogRepository.save(c);
        auditService.log(me, "收费目录", active ? "启用" : "停用", c.getId(), c.getCode(), null, c.getName());
        return c;
    }

    public List<CatalogCategory> categories() {
        return List.of(CatalogCategory.values());
    }
}
