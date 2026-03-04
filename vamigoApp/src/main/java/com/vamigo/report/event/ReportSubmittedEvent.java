package com.vamigo.report.event;

import com.vamigo.report.ReportReason;
import com.vamigo.report.ReportTargetType;

import java.time.Instant;

public record ReportSubmittedEvent(
        Long reportId,
        Long authorId,
        String authorEmail,
        ReportTargetType targetType,
        Long targetId,
        ReportReason reason,
        String comment,
        Instant createdAt
) {}
