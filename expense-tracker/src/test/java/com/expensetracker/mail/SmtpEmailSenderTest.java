package com.expensetracker.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SmtpEmailSenderTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpEmailSender sender = new SmtpEmailSender(mailSender, "SpendLedger <no-reply@spendledger.local>");

    private MimeMessage newMessage() {
        MimeMessage m = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(m);
        return m;
    }

    @Test
    void sendsMultipartMessageWithFromToAndSubject() throws Exception {
        MimeMessage m = newMessage();

        sender.send("user@example.com", "Hello", "plain body", "<b>html body</b>");

        verify(mailSender).send(m);
        assertThat(m.getRecipients(MimeMessage.RecipientType.TO)[0].toString()).isEqualTo("user@example.com");
        assertThat(m.getFrom()[0].toString()).contains("no-reply@spendledger.local");
        assertThat(m.getSubject()).isEqualTo("Hello");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.writeTo(out);
        String raw = out.toString("UTF-8");
        assertThat(raw).contains("multipart/", "text/plain", "text/html");
    }

    @Test
    void deliveryFailureIsPropagated() {
        MimeMessage m = newMessage();
        doThrow(new MailSendException("smtp down")).when(mailSender).send(m);
        assertThatThrownBy(() -> sender.send("u@example.com", "s", "t", "h")).isInstanceOf(MailSendException.class);
    }
}
