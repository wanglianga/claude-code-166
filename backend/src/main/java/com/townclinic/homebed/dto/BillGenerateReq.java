package com.townclinic.homebed.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** 生成账单：指定申请与账期（不传账期则建床日至今） */
public record BillGenerateReq(
        @NotNull Long applicationId,
        LocalDate start,
        LocalDate end
) {}
