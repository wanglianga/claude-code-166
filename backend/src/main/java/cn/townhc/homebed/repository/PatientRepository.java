package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByFamilyUserId(Long familyUserId);
}
