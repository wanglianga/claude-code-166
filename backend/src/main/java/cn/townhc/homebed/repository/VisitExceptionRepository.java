package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.VisitException;
import cn.townhc.homebed.domain.enums.DomainEnums.ExceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitExceptionRepository extends JpaRepository<VisitException, Long> {
    List<VisitException> findByVisitId(Long visitId);
    List<VisitException> findByStatusOrderByCreatedAtDesc(ExceptionStatus status);
    List<VisitException> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
