package cn.townhc.homebed.service;

import cn.townhc.homebed.domain.entity.AuditLog;
import cn.townhc.homebed.repository.AuditLogRepository;
import cn.townhc.homebed.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(LoginUser user, String module, String action, Long bizId, String bizNo,
                    Long patientId, String detail) {
        AuditLog log = new AuditLog();
        log.setModule(module);
        log.setAction(action);
        log.setBizId(bizId);
        log.setBizNo(bizNo);
        log.setPatientId(patientId);
        if (user != null) {
            log.setOperatorId(user.id());
            log.setOperatorName(user.name());
            log.setOperatorRole(user.role().name());
        }
        log.setDetail(detail);
        log.setIp(currentIp());
        log.setOccurredAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String forwarded = req.getHeader("X-Forwarded-For");
                return forwarded != null ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
