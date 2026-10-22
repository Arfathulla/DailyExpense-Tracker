package com.expensetracker.scheduler;

import com.expensetracker.dto.Dtos.CategoryTotal;
import com.expensetracker.dto.Dtos.Summary;
import com.expensetracker.entity.TxnType;
import com.expensetracker.mail.EmailSender;
import com.expensetracker.mail.SummaryEmailComposer;
import com.expensetracker.mail.SummaryEmailComposer.Composed;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

/**
 * Emails last month's summary to every user on the 1st at 08:00.
 * With {@code app.mail.enabled=false} (the default) the {@link EmailSender} only logs the message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlySummaryJob {
    private static final int TOP_CATEGORIES = 5;

    private final UserRepository users;
    private final AnalyticsService analytics;
    private final SummaryEmailComposer composer;
    private final EmailSender emailSender;

    @Scheduled(cron = "0 0 8 1 * *")
    public void run() {
        sendSummaries(YearMonth.now().minusMonths(1));
    }

    /** Returns how many emails were sent. A failure for one user never stops the others. */
    public int sendSummaries(YearMonth month) {
        int sent = 0;
        for (var u : users.findAll()) {
            try {
                Summary s = analytics.summary(u.getId(), month);
                if (s.income().signum() == 0 && s.expense().signum() == 0) continue; // nothing to report
                List<CategoryTotal> top = analytics.byCategory(u.getId(), month, TxnType.EXPENSE).stream()
                        .limit(TOP_CATEGORIES).toList();
                Composed mail = composer.compose(u.getName(), month, s, top);
                emailSender.send(u.getEmail(), mail.subject(), mail.text(), mail.html());
                sent++;
            } catch (Exception e) {
                log.error("Monthly summary failed for user {} ({}): {}", u.getId(), month, e.toString());
            }
        }
        log.info("Monthly summary job finished for {}: {} email(s) sent", month, sent);
        return sent;
    }
}
