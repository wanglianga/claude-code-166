package com.townclinic.homebed.dto;

import java.math.BigDecimal;

public record QuestionReplyReq(Long questionId, String reply, boolean resolved, BigDecimal refundAmount) {}
