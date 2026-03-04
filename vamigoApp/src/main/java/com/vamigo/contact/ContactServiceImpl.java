package com.vamigo.contact;

import com.vamigo.contact.dto.ContactRequest;
import com.vamigo.email.BrevoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class ContactServiceImpl implements ContactService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactServiceImpl.class);

    private final BrevoClient brevoClient;
    private final ContactRateLimiter rateLimiter;
    private final String contactAddress;
    private final String senderAddress;
    private final String senderName;

    public ContactServiceImpl(BrevoClient brevoClient,
                              ContactRateLimiter rateLimiter,
                              @Value("${app.email.contact-address}") String contactAddress,
                              @Value("${app.email.sender-address}") String senderAddress,
                              @Value("${app.email.sender-name}") String senderName) {
        this.brevoClient = brevoClient;
        this.rateLimiter = rateLimiter;
        this.contactAddress = contactAddress;
        this.senderAddress = senderAddress;
        this.senderName = senderName;
    }

    @Override
    public void submitContactForm(Long userId, ContactRequest request) {
        rateLimiter.checkAndRecord(userId);

        String subject = "Contact Form: from " + request.email();
        String html = buildHtml(userId, request);
        String text = buildPlainText(userId, request);

        brevoClient.sendHtmlEmail(
                contactAddress, "Vamigo Contact",
                senderAddress, senderName,
                request.email(),
                subject, html, text);

        LOGGER.info("Contact form submitted by user {} ({})", userId, request.email());
    }

    private String buildHtml(Long userId, ContactRequest request) {
        var sb = new StringBuilder();
        sb.append("<h2>Contact Form Submission</h2>");
        sb.append("<table style=\"border-collapse: collapse; width: 100%;\">");
        sb.append("<tr><td><strong>User ID:</strong></td><td>").append(userId).append("</td></tr>");
        sb.append("<tr><td><strong>Email:</strong></td><td>").append(esc(request.email())).append("</td></tr>");
        sb.append("<tr><td><strong>Phone:</strong></td><td>").append(orNA(esc(request.phone()))).append("</td></tr>");
        sb.append("<tr><td><strong>Message:</strong></td><td>").append(esc(request.message())).append("</td></tr>");

        if (request.appVersion() != null || request.platform() != null || request.locale() != null) {
            sb.append("<tr><td colspan=\"2\"><hr/><strong>Diagnostics</strong></td></tr>");
            if (request.appVersion() != null) {
                sb.append("<tr><td><strong>App Version:</strong></td><td>").append(esc(request.appVersion())).append("</td></tr>");
            }
            if (request.platform() != null) {
                sb.append("<tr><td><strong>Platform:</strong></td><td>").append(esc(request.platform())).append("</td></tr>");
            }
            if (request.locale() != null) {
                sb.append("<tr><td><strong>Locale:</strong></td><td>").append(esc(request.locale())).append("</td></tr>");
            }
        }

        sb.append("</table>");
        return sb.toString();
    }

    private String buildPlainText(Long userId, ContactRequest request) {
        var sb = new StringBuilder();
        sb.append("Contact Form Submission\n\n");
        sb.append("User ID: ").append(userId).append('\n');
        sb.append("Email: ").append(request.email()).append('\n');
        sb.append("Phone: ").append(orNA(request.phone())).append('\n');
        sb.append("Message: ").append(request.message()).append('\n');

        if (request.appVersion() != null || request.platform() != null || request.locale() != null) {
            sb.append("\n--- Diagnostics ---\n");
            if (request.appVersion() != null) sb.append("App Version: ").append(request.appVersion()).append('\n');
            if (request.platform() != null) sb.append("Platform: ").append(request.platform()).append('\n');
            if (request.locale() != null) sb.append("Locale: ").append(request.locale()).append('\n');
        }

        return sb.toString();
    }

    private static String esc(String value) {
        return value != null ? HtmlUtils.htmlEscape(value) : null;
    }

    private static String orNA(String value) {
        return value != null && !value.isBlank() ? value : "N/A";
    }
}
