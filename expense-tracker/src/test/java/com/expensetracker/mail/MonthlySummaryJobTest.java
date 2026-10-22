package com.expensetracker.mail;

import com.expensetracker.dto.Dtos.Summary;
import com.expensetracker.entity.TxnType;
import com.expensetracker.entity.User;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.scheduler.MonthlySummaryJob;
import com.expensetracker.service.AnalyticsService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MonthlySummaryJobTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AnalyticsService analytics = mock(AnalyticsService.class);
    private final EmailSender sender = mock(EmailSender.class);
    private final MonthlySummaryJob job =
            new MonthlySummaryJob(users, analytics, new SummaryEmailComposer("₹"), sender);
    private final YearMonth month = YearMonth.of(2026, 9);

    private static User user(long id, String email) {
        User u = new User();
        u.setId(id); u.setName("U" + id); u.setEmail(email);
        return u;
    }

    private void summary(long id, String income, String expense) {
        when(analytics.summary(id, month)).thenReturn(
                new Summary(new BigDecimal(income), new BigDecimal(expense), new BigDecimal(income).subtract(new BigDecimal(expense))));
        when(analytics.byCategory(id, month, TxnType.EXPENSE)).thenReturn(List.of());
    }

    @Test
    void skipsUsersWithNoActivity() {
        when(users.findAll()).thenReturn(List.of(user(1, "a@x.com"), user(2, "b@x.com")));
        summary(1, "0", "0");
        summary(2, "100", "40");

        assertThat(job.sendSummaries(month)).isEqualTo(1);
        verify(sender).send(eq("b@x.com"), contains("September 2026"), anyString(), anyString());
        verify(sender, never()).send(eq("a@x.com"), anyString(), anyString(), anyString());
    }

    @Test
    void oneFailingEmailDoesNotStopTheRest() {
        when(users.findAll()).thenReturn(List.of(user(1, "a@x.com"), user(2, "b@x.com")));
        summary(1, "10", "5");
        summary(2, "10", "5");
        doThrow(new RuntimeException("smtp down")).when(sender).send(eq("a@x.com"), anyString(), anyString(), anyString());

        assertThat(job.sendSummaries(month)).isEqualTo(1);
        verify(sender).send(eq("b@x.com"), anyString(), anyString(), anyString());
    }
}
