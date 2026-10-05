package com.townclinic.homebed.dto;

import com.townclinic.homebed.common.DomainEnums.PlanStatus;
import com.townclinic.homebed.common.DomainEnums.VisitAbortType;

public record PlanCancelReq(VisitAbortType abortType, String note) {}
