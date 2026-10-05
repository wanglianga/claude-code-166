package com.townclinic.homebed.dto;

public record QuestionReq(Long billId, Long billItemId, Long applicationId, String content) {}
