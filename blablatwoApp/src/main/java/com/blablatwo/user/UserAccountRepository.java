package com.blablatwo.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    @Modifying
    @Query(value = "INSERT INTO user_account (id, email, status, created_at, updated_at, version) " +
                   "SELECT :id, :email, :status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0 " +
                   "WHERE NOT EXISTS (SELECT 1 FROM user_account WHERE id = :id)",
           nativeQuery = true)
    void insertSystemUserIfNotExists(@Param("id") Long id,
                                     @Param("email") String email,
                                     @Param("status") String status);

    @Modifying
    @Query(value = "INSERT INTO user_account_roles (user_account_id, role) " +
                   "SELECT :userId, :role " +
                   "WHERE NOT EXISTS (SELECT 1 FROM user_account_roles WHERE user_account_id = :userId AND role = :role)",
           nativeQuery = true)
    void insertRoleIfNotExists(@Param("userId") Long userId,
                               @Param("role") String role);
}
