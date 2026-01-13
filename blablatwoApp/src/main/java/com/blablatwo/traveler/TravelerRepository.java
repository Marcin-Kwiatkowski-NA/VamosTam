package com.blablatwo.traveler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TravelerRepository extends JpaRepository<Traveler, Long> {
    Optional<Traveler> findByUsername(String username);

    Optional<Traveler> findByEmail(String email);

    Optional<Traveler> findByGoogleUser_GoogleId(String googleId);

    @Modifying
    @Query(value = "INSERT INTO traveler (id, username, email, name, role, enabled, version) " +
                   "SELECT :id, :username, :email, :name, :role, :enabled, 0 " +
                   "WHERE NOT EXISTS (SELECT 1 FROM traveler WHERE id = :id)",
           nativeQuery = true)
    void insertFacebookProxyIfNotExists(@Param("id") Long id,
                                        @Param("username") String username,
                                        @Param("email") String email,
                                        @Param("name") String name,
                                        @Param("role") String role,
                                        @Param("enabled") Integer enabled);
}