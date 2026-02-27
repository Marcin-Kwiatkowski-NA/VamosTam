package com.blablatwo.report.dto;

import com.blablatwo.report.ReportReason;
import com.blablatwo.report.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitReportRequest(
        @NotNull ReportTargetType targetType,
        @NotNull Long targetId,
        @NotNull ReportReason reason,
        @Size(max = 500) String comment
) {}
