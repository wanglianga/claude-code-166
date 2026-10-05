package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    AuthToken findByToken(String token);
    void deleteByToken(String token);
}
