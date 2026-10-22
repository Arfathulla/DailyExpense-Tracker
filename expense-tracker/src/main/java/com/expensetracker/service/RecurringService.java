package com.expensetracker.service;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.RecurringTransaction;
import com.expensetracker.entity.Transaction;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.NotFoundException;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.RecurringRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringService {
    private final RecurringRepository recurring;
    private final CategoryRepository categories;
    private final TransactionRepository txns;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<RecurringResponse> list(Long uid) {
        return recurring.findByUserIdOrderByDayOfMonthAsc(uid).stream().map(RecurringService::toResponse).toList();
    }

    @Transactional
    public RecurringResponse create(Long uid, RecurringRequest r) {
        Category c = categories.findByIdAndUserId(r.categoryId(), uid)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (c.getType() != r.type()) throw new BadRequestException("Category type does not match the transaction type");
        RecurringTransaction rt = new RecurringTransaction();
        rt.setUser(users.getReferenceById(uid));
        rt.setCategory(c);
        rt.setAmount(r.amount());
        rt.setType(r.type());
        rt.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        rt.setDayOfMonth(r.dayOfMonth());
        return toResponse(recurring.save(rt));
    }

    @Transactional
    public void delete(Long uid, Long id) {
        recurring.delete(recurring.findByIdAndUserId(id, uid).orElseThrow(() -> new NotFoundException("Recurring item not found")));
    }

    /**
     * Creates this month's transaction for every active rule whose day has arrived.
     * Safe to run repeatedly: lastRunMonth prevents duplicates and also catches up if the app was down.
     * Days like 31 fall back to the last day of shorter months.
     */
    @Transactional
    public int applyDue(LocalDate today) {
        String ym = YearMonth.from(today).toString();
        int created = 0;
        for (RecurringTransaction r : recurring.findByActiveTrue()) {
            int day = Math.min(r.getDayOfMonth(), today.lengthOfMonth());
            if (today.getDayOfMonth() < day || ym.equals(r.getLastRunMonth())) continue;

            Transaction t = new Transaction();
            t.setUser(r.getUser());
            t.setCategory(r.getCategory());
            t.setAmount(r.getAmount());
            t.setType(r.getType());
            t.setNote(r.getNote() == null ? "Recurring" : r.getNote() + " (recurring)");
            t.setTxnDate(today.withDayOfMonth(day));
            txns.save(t);
            r.setLastRunMonth(ym);
            created++;
        }
        return created;
    }

    static RecurringResponse toResponse(RecurringTransaction r) {
        return new RecurringResponse(r.getId(), r.getCategory().getId(), r.getCategory().getName(),
                r.getAmount(), r.getType(), r.getNote(), r.getDayOfMonth(), r.isActive());
    }
}
