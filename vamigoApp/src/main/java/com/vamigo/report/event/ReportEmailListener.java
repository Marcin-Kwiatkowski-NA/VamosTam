package com.vamigo.report.event;

import com.vamigo.email.BrevoClient;
import com.vamigo.email.EmailSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

@Component
public class ReportEmailListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportEmailListener.class);

    private final BrevoClient brevoClient;
    private final String supportAddress;
    private final String senderAddress;
    private final String senderName;

    public ReportEmailListener(BrevoClient brevoClient,
                               @Value("${app.email.support-address}") String supportAddress,
                               @Value("${app.email.sender-address}") String senderAddress,
                               @Value("${app.email.sender-name}") String senderName) {
        this.brevoClient = brevoClient;
        this.supportAddress = supportAddress;
        this.senderAddress = senderAddress;
        this.senderName = senderName;
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportSubmitted(ReportSubmittedEvent event) {
        try {
            String subject = "New Report: " + event.targetType() + " #" + event.targetId();
            String html = buildHtml(event);
            String text = buildPlainText(event);

            brevoClient.sendHtmlEmail(
                    supportAddress, "Vamigo Support",
                    senderAddress, senderName,
                    event.authorEmail(),
                    subject, html, text);
        } catch (EmailSendException e) {
            LOGGER.error("Failed to send report notification for report {}: {}",
                    event.reportId(), e.getMessage());
        }
    }

    private String buildHtml(ReportSubmittedEvent event) {
        String escapedComment = event.comment() != null
                ? HtmlUtils.htmlEscape(event.comment())
                : "N/A";

        return """
                <h2>New Report Submitted</h2>
                <table style="border-collapse: collapse; width: 100%%;">
                  <tr><td><strong>Report ID:</strong></td><td>%d</td></tr>
                  <tr><td><strong>Target Type:</strong></td><td>%s</td></tr>
                  <tr><td><strong>Target ID:</strong></td><td>%d</td></tr>
                  <tr><td><strong>Reason:</strong></td><td>%s</td></tr>
                  <tr><td><strong>Comment:</strong></td><td>%s</td></tr>
                  <tr><td><strong>Reporter ID:</strong></td><td>%d</td></tr>
                  <tr><td><strong>Reporter Email:</strong></td><td>%s</td></tr>
                  <tr><td><strong>Submitted At:</strong></td><td>%s</td></tr>
                </table>
                """.formatted(
                event.reportId(),
                event.targetType(),
                event.targetId(),
                event.reason(),
                escapedComment,
                event.authorId(),
                HtmlUtils.htmlEscape(event.authorEmail()),
                event.createdAt()
        );
    }

    private String buildPlainText(ReportSubmittedEvent event) {
        return """
                New Report Submitted

                Report ID: %d
                Target Type: %s
                Target ID: %d
                Reason: %s
                Comment: %s
                Reporter ID: %d
                Reporter Email: %s
                Submitted At: %s
                """.formatted(
                event.reportId(),
                event.targetType(),
                event.targetId(),
                event.reason(),
                event.comment() != null ? event.comment() : "N/A",
                event.authorId(),
                event.authorEmail(),
                event.createdAt()
        );
    }
}
