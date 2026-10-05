package com.townclinic.homebed.dto;

public record ReviewReq(Long visitId, String reviewNote, boolean escalated) {}
