package com.expensetracker.scheduler;

import com.expensetracker.service.RecurringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionJob {
    private final RecurringService recurringService;

    /** Runs every day at 00:05 server time. */
    @Scheduled(cron = "0 5 0 * * *")
    public void run() {
        int created = recurringService.applyDue(LocalDate.now());
        log.info("Recurring job finished: {} transaction(s) created", created);
    }
}
