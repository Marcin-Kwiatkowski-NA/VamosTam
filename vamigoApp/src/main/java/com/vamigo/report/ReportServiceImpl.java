package com.vamigo.report;

import com.vamigo.report.dto.SubmitReportRequest;
import com.vamigo.report.event.ReportSubmittedEvent;
import com.vamigo.report.exception.AlreadyReportedException;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserAccountRepository userAccountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReportServiceImpl(ReportRepository reportRepository,
                             UserAccountRepository userAccountRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.reportRepository = reportRepository;
        this.userAccountRepository = userAccountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void submitReport(Long authorId, String authorEmail, SubmitReportRequest request) {
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

        eventPublisher.publishEvent(new ReportSubmittedEvent(
                report.getId(),
                authorId,
                authorEmail,
                request.targetType(),
                request.targetId(),
                request.reason(),
                request.comment(),
                report.getCreatedAt()
        ));
    }
}
