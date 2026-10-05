package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.VisitPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitPhotoRepository extends JpaRepository<VisitPhoto, Long> {
    List<VisitPhoto> findByVisitId(Long visitId);
}
