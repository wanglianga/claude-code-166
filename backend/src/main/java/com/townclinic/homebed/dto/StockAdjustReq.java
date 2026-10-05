package com.townclinic.homebed.dto;

import java.math.BigDecimal;

/** 库存调整（入库为正、盘亏为负），并记录原因 */
public record StockAdjustReq(BigDecimal changeQty, String reason) {}
