package com.expensetracker.integration;

import com.expensetracker.entity.Category;
import com.expensetracker.entity.RecurringTransaction;
import com.expensetracker.entity.TxnType;
import com.expensetracker.entity.User;
import com.expensetracker.mail.EmailSender;
import com.expensetracker.repository.*;
import com.expensetracker.scheduler.MonthlySummaryJob;
import com.expensetracker.service.AuthService;
import com.expensetracker.dto.Dtos.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Scheduled jobs against real MySQL. The email sender is mocked; SMTP itself is covered in unit tests. */
class RecurringAndSummaryJobIT extends AbstractMySqlIT {
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired TransactionRepository txns;
    @Autowired RecurringRepository recurring;
    @Autowired com.expensetracker.service.RecurringService recurringService;
    @Autowired MonthlySummaryJob summaryJob;
    @MockBean EmailSender emailSender;

    private User newUser() {
        String email = UUID.randomUUID() + "@x.com";
        auth.register(new RegisterRequest("Job Tester", email, "secret1"));
        return users.findByEmail(email).orElseThrow();
    }

    private Category category(User u, String name) {
        return categories.findByUserIdOrderByNameAsc(u.getId()).stream()
                .filter(c -> c.getName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void recurringRuleCreatesOneTransactionPerMonthEvenIfRunTwice() {
        User u = newUser();
        Category rent = category(u, "Rent");
        RecurringTransaction r = new RecurringTransaction();
        r.setUser(u); r.setCategory(rent); r.setAmount(new BigDecimal("500.00"));
        r.setType(TxnType.EXPENSE); r.setNote("Rent"); r.setDayOfMonth(1);
        recurring.save(r);

        LocalDate today = LocalDate.now();
        int first = recurringService.applyDue(today);
        int second = recurringService.applyDue(today);

        assertThat(first).isGreaterThanOrEqualTo(1);
        assertThat(second).isZero();
        assertThat(txns.sumForCategory(u.getId(), rent.getId(), TxnType.EXPENSE,
                today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth())))
                .isEqualByComparingTo("500.00");
    }

    @Test
    void monthlySummaryEmailsOnlyUsersWithActivity() {
        User active = newUser();
        User idle = newUser();
        YearMonth month = YearMonth.now().minusMonths(1);
        com.expensetracker.entity.Transaction t = new com.expensetracker.entity.Transaction();
        Category food = category(active, "Food");
        t.setUser(active); t.setCategory(food); t.setAmount(new BigDecimal("75.00"));
        t.setType(TxnType.EXPENSE); t.setTxnDate(month.atDay(10));
        txns.save(t);

        summaryJob.sendSummaries(month);

        verify(emailSender).send(eq(active.getEmail()), contains("spending summary"), contains("75.00"), contains("75.00"));
        verify(emailSender, never()).send(eq(idle.getEmail()), anyString(), anyString(), anyString());
    }
}
