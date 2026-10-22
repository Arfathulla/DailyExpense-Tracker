package com.expensetracker.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Default when email is not configured: writes the message to the log instead of sending it. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {
    @Override
    public void send(String to, String subject, String textBody, String htmlBody) {
        log.info("Email disabled (app.mail.enabled=false). Would send to {}: {}\n{}", to, subject, textBody);
    }
}
