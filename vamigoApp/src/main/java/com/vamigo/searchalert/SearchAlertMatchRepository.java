package com.vamigo.searchalert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchAlertMatchRepository extends JpaRepository<SearchAlertMatch, Long> {

    @Query("""
            SELECT m FROM SearchAlertMatch m
            JOIN FETCH m.savedSearch ss
            WHERE m.pushSent = false
            ORDER BY ss.id, m.createdAt
            """)
    List<SearchAlertMatch> findUnsentPush();

    @Query("""
            SELECT m FROM SearchAlertMatch m
            JOIN FETCH m.savedSearch ss
            WHERE m.emailSent = false
            ORDER BY ss.id, m.createdAt
            """)
    List<SearchAlertMatch> findUnsentEmail();

    @Modifying
    @Query("DELETE FROM SearchAlertMatch m WHERE m.id IN :ids")
    void deleteByIds(@Param("ids") List<Long> ids);

    void deleteBySavedSearchId(Long savedSearchId);
}
