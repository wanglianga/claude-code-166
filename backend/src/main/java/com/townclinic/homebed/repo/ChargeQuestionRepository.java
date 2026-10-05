package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.ChargeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargeQuestionRepository extends JpaRepository<ChargeQuestion, Long> {
    List<ChargeQuestion> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
    List<ChargeQuestion> findByFamilyUserIdOrderByCreatedAtDesc(Long familyUserId);
    List<ChargeQuestion> findAllByOrderByCreatedAtDesc();
}
