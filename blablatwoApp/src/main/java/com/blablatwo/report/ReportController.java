package com.blablatwo.report;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.report.dto.SubmitReportRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/reports")
    public ResponseEntity<Void> submitReport(
            @Valid @RequestBody SubmitReportRequest request,
            @AuthenticationPrincipal AppPrincipal principal) {
        reportService.submitReport(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
