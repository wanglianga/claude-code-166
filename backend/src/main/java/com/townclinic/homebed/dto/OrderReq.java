package com.townclinic.homebed.dto;

import com.townclinic.homebed.common.DomainEnums.OrderType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OrderReq(
        @NotNull Long visitId,
        @NotNull OrderType orderType,
        @NotNull String content,
        LocalDate targetDate,
        String targetOrg
) {}
