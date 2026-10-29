package com.gopesh.identity.user.repository;

import com.gopesh.identity.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndTenantId(String email, String tenantId);

    boolean existsByEmailAndTenantId(String email, String tenantId);

    Page<User> findAllByTenantId(String tenantId, Pageable pageable);

    long countByTenantId(String tenantId);
}
