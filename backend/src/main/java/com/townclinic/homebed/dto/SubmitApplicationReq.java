package com.townclinic.homebed.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 家属提交家庭病床申请 */
public record SubmitApplicationReq(
        @NotNull Long patientId,
        String caregiverName,
        String caregiverRelation,
        String caregiverPhone,
        String reason,
        List<String> requestedServices,
        String expectedFrequency
) {}
