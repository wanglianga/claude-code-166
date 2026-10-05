package com.townclinic.homebed.service;

import com.townclinic.homebed.common.ApiException;
import com.townclinic.homebed.common.DomainEnums.Role;
import com.townclinic.homebed.dto.OrderReq;
import com.townclinic.homebed.entity.*;
import com.townclinic.homebed.repo.*;
import com.townclinic.homebed.security.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 医生根据巡诊记录调整处方、安排复诊、建议转诊、调整护理方案。 */
@Service
public class DoctorOrderService {

    private final DoctorOrderRepository orderRepo;
    private final VisitRepository visitRepo;
    private final AuditService audit;

    public DoctorOrderService(DoctorOrderRepository orderRepo, VisitRepository visitRepo, AuditService audit) {
        this.orderRepo = orderRepo;
        this.visitRepo = visitRepo;
        this.audit = audit;
    }

    @Transactional
    public DoctorOrder create(OrderReq req) {
        AppUser doctor = Roles.require(Role.DOCTOR);
        Visit v = visitRepo.findById(req.visitId()).orElseThrow(() -> new ApiException("巡诊记录不存在"));
        DoctorOrder o = new DoctorOrder();
        o.setVisitId(v.getId());
        o.setApplicationId(v.getApplicationId());
        o.setPatientId(v.getPatientId());
        o.setDoctorId(doctor.getId());
        o.setOrderType(req.orderType());
        o.setContent(req.content());
        o.setTargetDate(req.targetDate());
        o.setTargetOrg(req.targetOrg());
        o = orderRepo.save(o);
        audit.log(v.getApplicationId(), v.getId(), null, "DOCTOR_ORDER",
                "医生 " + doctor.getName() + " 下达医嘱[" + req.orderType() + "]：" + req.content()
                        + (req.targetOrg() != null ? "；机构：" + req.targetOrg() : ""));
        return o;
    }

    @Transactional
    public DoctorOrder updateExec(Long orderId, String status, String note) {
        Roles.require(Role.NURSE, Role.DOCTOR, Role.ADMIN);
        DoctorOrder o = orderRepo.findById(orderId).orElseThrow(() -> new ApiException("医嘱不存在"));
        o.setExecStatus(status);
        o.setExecNote(note);
        orderRepo.save(o);
        audit.log(o.getApplicationId(), o.getVisitId(), null, "ORDER_EXEC",
                "医嘱 #" + orderId + " 执行状态更新为 " + status + "：" + (note == null ? "" : note));
        return o;
    }

    @Transactional(readOnly = true)
    public List<DoctorOrder> listByApplication(Long applicationId) {
        Roles.requireLogin();
        return orderRepo.findByApplicationIdOrderByCreatedAtDesc(applicationId);
    }
}
