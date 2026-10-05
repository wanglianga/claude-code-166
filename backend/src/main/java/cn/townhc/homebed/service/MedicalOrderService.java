package cn.townhc.homebed.service;

import cn.townhc.homebed.common.BusinessException;
import cn.townhc.homebed.domain.entity.MedicalOrder;
import cn.townhc.homebed.domain.entity.Patient;
import cn.townhc.homebed.domain.entity.SysUser;
import cn.townhc.homebed.domain.entity.Visit;
import cn.townhc.homebed.domain.enums.DomainEnums.*;
import cn.townhc.homebed.repository.MedicalOrderRepository;
import cn.townhc.homebed.repository.PatientRepository;
import cn.townhc.homebed.repository.SysUserRepository;
import cn.townhc.homebed.repository.VisitRepository;
import cn.townhc.homebed.security.LoginUser;
import cn.townhc.homebed.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalOrderService {

    private final MedicalOrderRepository orderRepository;
    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final SysUserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<MedicalOrder> list(String status, Long patientId, Long visitId) {
        if (visitId != null) return orderRepository.findByVisitId(visitId);
        if (patientId != null) return orderRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        LoginUser me = SecurityUtils.current();
        if (me.role() == Role.FAMILY_DOCTOR) {
            List<MedicalOrder> all = orderRepository.findAll().stream()
                    .filter(o -> o.getDoctor() != null && o.getDoctor().getId().equals(me.id()))
                    .sorted(Comparator.comparing(MedicalOrder::getCreatedAt).reversed()).toList();
            return filterStatus(all, status);
        }
        if (status != null && !status.isBlank()) {
            return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.valueOf(status));
        }
        List<MedicalOrder> all = orderRepository.findAll();
        all.sort(Comparator.comparing(MedicalOrder::getCreatedAt).reversed());
        return all;
    }

    private List<MedicalOrder> filterStatus(List<MedicalOrder> list, String status) {
        if (status == null || status.isBlank()) return list;
        OrderStatus s = OrderStatus.valueOf(status);
        return list.stream().filter(o -> o.getStatus() == s).toList();
    }

    @Transactional
    public MedicalOrder create(Long visitId, Long patientId, OrderType orderType, String content,
                               String diagnosisBasis, Boolean coldChainRequired, String coldChainNote) {
        LoginUser me = SecurityUtils.current();
        if (me.role() != Role.FAMILY_DOCTOR && me.role() != Role.ADMIN) {
            throw new BusinessException(403, "仅家庭医生可下达医嘱");
        }
        MedicalOrder order = new MedicalOrder();
        order.setOrderNo(ApplicationService.generateNo("YZ"));
        SysUser doctor = userRepository.findById(me.id()).orElseThrow();
        order.setDoctor(doctor);
        Patient patient;
        if (visitId != null) {
            Visit visit = visitRepository.findById(visitId)
                    .orElseThrow(() -> new BusinessException(404, "巡诊记录不存在"));
            order.setVisit(visit);
            order.setPlan(visit.getPlan());
            patient = visit.getPatient();
        } else if (patientId != null) {
            patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new BusinessException(404, "患者不存在"));
        } else {
            throw new BusinessException("缺少巡诊或患者信息");
        }
        order.setPatient(patient);
        order.setOrderType(orderType);
        order.setContent(content);
        order.setDiagnosisBasis(diagnosisBasis);
        order.setStatus(OrderStatus.PROPOSED);
        order.setColdChainRequired(Boolean.TRUE.equals(coldChainRequired));
        order.setColdChainNote(coldChainNote);
        order = orderRepository.save(order);

        // 转诊/病情恶化类医嘱需要远程复核
        auditService.log(me, "医嘱", "下达医嘱·" + orderType.label, order.getId(), order.getOrderNo(),
                order.getPatient().getId(), content);
        return order;
    }

    /** 远程复核：由另一名医生（通常是上级/值班医生）远程复核医嘱 */
    @Transactional
    public MedicalOrder remoteReview(Long orderId, boolean approved, String comment) {
        LoginUser me = SecurityUtils.current();
        if (me.role() != Role.FAMILY_DOCTOR && me.role() != Role.ADMIN) {
            throw new BusinessException(403, "仅医生可远程复核");
        }
        MedicalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "医嘱不存在"));
        if (order.getDoctor().getId().equals(me.id())) {
            throw new BusinessException("不能复核本人下达的医嘱，请由另一位医生远程复核");
        }
        if (order.getStatus() != OrderStatus.PROPOSED) {
            throw new BusinessException("仅待复核医嘱可远程复核");
        }
        order.setReviewer(userRepository.findById(me.id()).orElse(null));
        order.setReviewComment(comment);
        order.setReviewedAt(LocalDateTime.now());
        order.setStatus(approved ? OrderStatus.REMOTE_REVIEWED : OrderStatus.PROPOSED);
        if (!approved) {
            order.setContent(order.getContent() + "\n【远程复核退回】" + comment);
        }
        order = orderRepository.save(order);
        auditService.log(me, "医嘱", approved ? "远程复核通过" : "远程复核退回",
                order.getId(), order.getOrderNo(), order.getPatient().getId(), comment);
        return order;
    }

    @Transactional
    public MedicalOrder execute(Long orderId, String note) {
        LoginUser me = SecurityUtils.current();
        MedicalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "医嘱不存在"));
        order.setStatus(OrderStatus.EXECUTED);
        order.setExecutionNote(note);
        order.setExecutedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        auditService.log(me, "医嘱", "执行确认", order.getId(), order.getOrderNo(),
                order.getPatient().getId(), note);
        return order;
    }

    @Transactional
    public MedicalOrder voidOrder(Long orderId, String reason) {
        LoginUser me = SecurityUtils.current();
        MedicalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "医嘱不存在"));
        order.setStatus(OrderStatus.VOID);
        order.setExecutionNote("作废原因：" + reason);
        order = orderRepository.save(order);
        auditService.log(me, "医嘱", "作废", order.getId(), order.getOrderNo(),
                order.getPatient().getId(), reason);
        return order;
    }
}
