package com.vamigo.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByAuthorIdAndTargetTypeAndTargetId(Long authorId, ReportTargetType targetType, Long targetId);
}
