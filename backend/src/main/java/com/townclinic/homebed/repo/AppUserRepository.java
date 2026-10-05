package com.townclinic.homebed.repo;

import com.townclinic.homebed.entity.AppUser;
import com.townclinic.homebed.common.DomainEnums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    List<AppUser> findByRole(Role role);
    List<AppUser> findByRoleAndEnabledTrue(Role role);
}
