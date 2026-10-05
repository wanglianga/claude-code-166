package com.townclinic.homebed.dto;

import com.townclinic.homebed.common.DomainEnums.RiskEventType;
import jakarta.validation.constraints.NotNull;

public record RiskEventReq(
        @NotNull Long applicationId,
        Long visitId,
        @NotNull RiskEventType eventType,
        @NotNull String description
) {}
