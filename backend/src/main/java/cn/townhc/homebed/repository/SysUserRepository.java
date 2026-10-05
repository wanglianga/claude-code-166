package cn.townhc.homebed.repository;

import cn.townhc.homebed.domain.entity.SysUser;
import cn.townhc.homebed.domain.enums.DomainEnums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
    List<SysUser> findByRoleAndActiveTrue(Role role);
    List<SysUser> findByActiveTrue();
}
