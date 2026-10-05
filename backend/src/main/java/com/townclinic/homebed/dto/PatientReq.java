package com.townclinic.homebed.dto;

import jakarta.validation.constraints.NotNull;

public record PatientReq(
        String name,
        String gender,
        Integer age,
        String idCard,
        String phone,
        String address,
        Double lng,
        Double lat,
        String insuranceType,
        String insuranceNo,
        String diagnosis,
        String mobility
) {}
