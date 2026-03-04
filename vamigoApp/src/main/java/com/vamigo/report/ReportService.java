package com.vamigo.report;

import com.vamigo.report.dto.SubmitReportRequest;

public interface ReportService {

    void submitReport(Long authorId, String authorEmail, SubmitReportRequest request);
}
