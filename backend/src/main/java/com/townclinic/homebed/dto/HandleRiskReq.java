package com.townclinic.homebed.dto;

public record HandleRiskReq(Long eventId, String handleNote, boolean close) {}
