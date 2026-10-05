package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
