package com.blablatwo.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    @Modifying
    @Query(value = "INSERT INTO user_profile (id, display_name, rides_given, rides_taken, rating_sum, rating_count) " +
                   "SELECT :id, :displayName, 0, 0, 0, 0 " +
                   "WHERE NOT EXISTS (SELECT 1 FROM user_profile WHERE id = :id)",
           nativeQuery = true)
    void insertProfileIfNotExists(@Param("id") Long id,
                                  @Param("displayName") String displayName);
}
