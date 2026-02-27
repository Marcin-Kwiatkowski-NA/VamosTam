package com.blablatwo.report;

import com.blablatwo.report.dto.SubmitReportRequest;
import com.blablatwo.report.exception.AlreadyReportedException;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserAccountRepository userAccountRepository;

    public ReportServiceImpl(ReportRepository reportRepository,
                             UserAccountRepository userAccountRepository) {
        this.reportRepository = reportRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional
    public void submitReport(Long authorId, SubmitReportRequest request) {
        boolean alreadyReported = reportRepository.existsByAuthorIdAndTargetTypeAndTargetId(
                authorId, request.targetType(), request.targetId());

        if (alreadyReported) {
            throw new AlreadyReportedException(request.targetType().name(), request.targetId());
        }

        UserAccount author = userAccountRepository.getReferenceById(authorId);

        Report report = Report.builder()
                .author(author)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason())
                .comment(request.comment())
                .build();

        reportRepository.save(report);
    }
}
