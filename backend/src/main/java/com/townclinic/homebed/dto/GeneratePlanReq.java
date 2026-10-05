package com.townclinic.homebed.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** 生成巡诊计划：在起止日期内按指定星期（1=周一..7=周日）排期 */
public record GeneratePlanReq(
        @NotNull Long applicationId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        /** 上门的星期，如 [1,3,5]；为空则每天 */
        List<Integer> weekdays,
        List<String> services
) {}
