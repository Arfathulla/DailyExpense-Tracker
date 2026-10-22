package com.expensetracker.config;

import com.expensetracker.dto.Dtos.RegisterRequest;
import com.expensetracker.entity.*;
import com.expensetracker.repository.*;
import com.expensetracker.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Creates demo@example.com / Demo@123 with a few months of data so the dashboard looks alive on first run. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DataSeeder implements CommandLineRunner {
    private final AuthService authService;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final TransactionRepository txns;
    private final BudgetRepository budgets;
    private final RecurringRepository recurring;

    @Override
    public void run(String... args) {
        String email = "demo@example.com";
        if (users.existsByEmail(email)) return;
        authService.register(new RegisterRequest("Demo User", email, "Demo@123"));
        User u = users.findByEmail(email).orElseThrow();
        Map<String, Category> cat = categories.findByUserIdOrderByNameAsc(u.getId()).stream()
                .collect(Collectors.toMap(Category::getName, Function.identity()));

        Random rnd = new Random(7);
        LocalDate today = LocalDate.now();
        List<Transaction> batch = new ArrayList<>();
        for (int back = 3; back >= 0; back--) {
            YearMonth ym = YearMonth.from(today).minusMonths(back);
            add(batch, u, cat.get("Salary"), "85000", ym, 1, "Monthly salary", today);
            add(batch, u, cat.get("Rent"), "22000", ym, 2, "House rent", today);
            add(batch, u, cat.get("Bills"), String.valueOf(2800 + rnd.nextInt(900)), ym, 5, "Electricity and internet", today);
            if (back % 2 == 0) add(batch, u, cat.get("Freelance"), "12000", ym, 12, "Website project", today);
            String[] food = {"Groceries", "Dinner out", "Lunch with team", "Swiggy order", "Weekend brunch", "Vegetables"};
            for (int i = 0; i < 6; i++)
                add(batch, u, cat.get("Food"), String.valueOf(300 + rnd.nextInt(1100)), ym, 3 + i * 4, food[i], today);
            add(batch, u, cat.get("Shopping"), String.valueOf(1500 + rnd.nextInt(3500)), ym, 14, "Clothes", today);
            add(batch, u, cat.get("Entertainment"), String.valueOf(400 + rnd.nextInt(900)), ym, 18, "Movie and snacks", today);
            add(batch, u, cat.get("Travel"), String.valueOf(800 + rnd.nextInt(2800)), ym, 21, "Cab and fuel", today);
            if (back == 1) add(batch, u, cat.get("Health"), "1800", ym, 9, "Doctor visit", today);
        }
        txns.saveAll(batch);

        String month = YearMonth.from(today).toString();
        budget(u, cat.get("Food"), month, "7500");
        budget(u, cat.get("Shopping"), month, "6000");
        budget(u, cat.get("Travel"), month, "4000");
        budget(u, cat.get("Entertainment"), month, "3000");
        budget(u, cat.get("Bills"), month, "4500");

        rule(u, cat.get("Rent"), "22000", 2, "House rent", month);
        rule(u, cat.get("Salary"), "85000", 1, "Monthly salary", month);
    }

    private void add(List<Transaction> list, User u, Category c, String amt, YearMonth ym, int day,
                     String note, LocalDate today) {
        LocalDate d = ym.atDay(Math.min(day, ym.lengthOfMonth()));
        if (d.isAfter(today)) return; // no future-dated demo data
        Transaction t = new Transaction();
        t.setUser(u); t.setCategory(c); t.setAmount(new BigDecimal(amt)); t.setType(c.getType());
        t.setNote(note); t.setTxnDate(d);
        list.add(t);
    }

    private void budget(User u, Category c, String month, String limit) {
        Budget b = new Budget();
        b.setUser(u); b.setCategory(c); b.setMonth(month); b.setLimitAmount(new BigDecimal(limit));
        budgets.save(b);
    }

    private void rule(User u, Category c, String amt, int day, String note, String lastRunMonth) {
        RecurringTransaction r = new RecurringTransaction();
        r.setUser(u); r.setCategory(c); r.setAmount(new BigDecimal(amt)); r.setType(c.getType());
        r.setNote(note); r.setDayOfMonth(day); r.setLastRunMonth(lastRunMonth); // already seeded this month
        recurring.save(r);
    }
}
