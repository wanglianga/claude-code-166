package com.townclinic.homebed.dto;

import java.util.List;

/** 家属确认账单：逐项确认 + 整体意见 */
public record ConfirmBillReq(String familyConfirmNote, List<Long> ackItemIds) {}
