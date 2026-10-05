package com.townclinic.homebed.dto;

import com.townclinic.homebed.common.DomainEnums.VisitAbortType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 护士提交一次巡诊记录（含生命体征、护理、耗材领用、家属签字） */
public record VisitCompleteReq(
        Double temperature,
        Integer systolic,
        Integer diastolic,
        Integer heartRate,
        Double bloodGlucose,
        Integer spo2,
        String pressureUlcer,
        String dressingChange,
        String catheterCare,
        String medicationReminder,
        String nursingNote,
        String abnormalNote,
        VisitAbortType abortType,
        String abortNote,
        Double coldChainTemp,
        /** 家属签字 dataURL（image/png） */
        String familySignature,
        String familySignName,
        List<ItemUseReq> items
) {
    public record ItemUseReq(@NotNull Long itemId, @NotNull java.math.BigDecimal quantity) {}
}
