package com.townclinic.homebed.dto;

import com.townclinic.homebed.common.DomainEnums.RiskLevel;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 家庭医生评估：病情/距离/照护人/医保资格/护理项目/风险 */
public record AssessReq(
        @NotNull Long applicationId,
        Double distanceKm,
        Boolean insuranceEligible,
        RiskLevel riskLevel,
        List<String> approvedServices,
        String approvedFrequency,
        String assessOpinion,
        /** 评估结论：true 建议建床，false 驳回 */
        boolean eligible,
        String rejectReason
) {}
