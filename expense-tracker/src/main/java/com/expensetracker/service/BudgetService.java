package com.expensetracker.service;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.entity.Budget;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.TxnType;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.NotFoundException;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {
    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final TransactionRepository txns;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long uid, String month) {
        YearMonth ym = YearMonth.parse(month);
        return budgets.findByUserIdAndMonthOrderByIdAsc(uid, month).stream()
                .map(b -> toResponse(uid, b, ym)).toList();
    }

    /** Creates a budget, or updates the limit if one already exists for that category and month. */
    @Transactional
    public BudgetResponse upsert(Long uid, BudgetRequest r) {
        Category c = categories.findByIdAndUserId(r.categoryId(), uid)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (c.getType() != TxnType.EXPENSE) throw new BadRequestException("Budgets can only be set for expense categories");
        Budget b = budgets.findByUserIdAndCategoryIdAndMonth(uid, c.getId(), r.month()).orElseGet(Budget::new);
        if (b.getId() == null) {
            b.setUser(users.getReferenceById(uid));
            b.setCategory(c);
            b.setMonth(r.month());
        }
        b.setLimitAmount(r.limitAmount());
        return toResponse(uid, budgets.save(b), YearMonth.parse(r.month()));
    }

    @Transactional
    public BudgetResponse update(Long uid, Long id, BudgetRequest r) {
        Budget b = budgets.findByIdAndUserId(id, uid).orElseThrow(() -> new NotFoundException("Budget not found"));
        b.setLimitAmount(r.limitAmount());
        return toResponse(uid, budgets.save(b), YearMonth.parse(b.getMonth()));
    }

    @Transactional
    public void delete(Long uid, Long id) {
        budgets.delete(budgets.findByIdAndUserId(id, uid).orElseThrow(() -> new NotFoundException("Budget not found")));
    }

    BudgetResponse toResponse(Long uid, Budget b, YearMonth ym) {
        BigDecimal spent = txns.sumForCategory(uid, b.getCategory().getId(), TxnType.EXPENSE,
                ym.atDay(1), ym.atEndOfMonth());
        double pct = spent.multiply(BigDecimal.valueOf(100))
                .divide(b.getLimitAmount(), 1, RoundingMode.HALF_UP).doubleValue();
        String status = pct >= 100 ? "EXCEEDED" : pct >= 80 ? "WARNING" : "OK";
        return new BudgetResponse(b.getId(), b.getCategory().getId(), b.getCategory().getName(),
                b.getMonth(), b.getLimitAmount(), spent, pct, status);
    }
}
