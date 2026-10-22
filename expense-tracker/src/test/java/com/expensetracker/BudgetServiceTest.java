package com.expensetracker;

import com.expensetracker.dto.Dtos.BudgetResponse;
import com.expensetracker.entity.Budget;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.TxnType;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.BudgetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {
    @Mock BudgetRepository budgets;
    @Mock CategoryRepository categories;
    @Mock TransactionRepository txns;
    @Mock UserRepository users;
    @InjectMocks BudgetService service;

    private BudgetResponse listWithSpent(String spent) {
        Category food = new Category();
        food.setId(1L);
        food.setName("Food");
        food.setType(TxnType.EXPENSE);
        Budget b = new Budget();
        b.setId(10L);
        b.setCategory(food);
        b.setMonth("2026-10");
        b.setLimitAmount(new BigDecimal("1000"));

        when(budgets.findByUserIdAndMonthOrderByIdAsc(1L, "2026-10")).thenReturn(List.of(b));
        when(txns.sumForCategory(eq(1L), eq(1L), eq(TxnType.EXPENSE), any(), any())).thenReturn(new BigDecimal(spent));
        return service.list(1L, "2026-10").get(0);
    }

    @Test
    void underEightyPercentIsOk() {
        assertEquals("OK", listWithSpent("500").status());
    }

    @Test
    void eightyPercentTriggersWarning() {
        BudgetResponse r = listWithSpent("850");
        assertEquals("WARNING", r.status());
        assertEquals(85.0, r.percentUsed());
    }

    @Test
    void overLimitIsExceeded() {
        assertEquals("EXCEEDED", listWithSpent("1200").status());
    }
}
