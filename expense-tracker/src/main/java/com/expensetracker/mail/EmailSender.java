package com.expensetracker.mail;

/** Sends one email with a plain-text body and an HTML alternative. Implementations decide how (SMTP, log only, ...). */
public interface EmailSender {
    void send(String to, String subject, String textBody, String htmlBody);
}
