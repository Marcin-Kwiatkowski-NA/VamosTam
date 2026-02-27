package com.blablatwo.report;

import com.blablatwo.report.dto.SubmitReportRequest;

public interface ReportService {

    void submitReport(Long authorId, SubmitReportRequest request);
}
