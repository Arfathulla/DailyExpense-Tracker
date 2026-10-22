package com.expensetracker.service;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.entity.TxnType;
import com.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {
    private final TransactionRepository txns;

    public Summary summary(Long uid, YearMonth ym) {
        BigDecimal income = txns.sumByType(uid, TxnType.INCOME, ym.atDay(1), ym.atEndOfMonth());
        BigDecimal expense = txns.sumByType(uid, TxnType.EXPENSE, ym.atDay(1), ym.atEndOfMonth());
        return new Summary(income, expense, income.subtract(expense));
    }

    public List<CategoryTotal> byCategory(Long uid, YearMonth ym, TxnType type) {
        return txns.sumByCategory(uid, type, ym.atDay(1), ym.atEndOfMonth()).stream()
                .map(r -> new CategoryTotal((String) r[0], (BigDecimal) r[1])).toList();
    }

    /** Income vs expense for each of the last N months (months with no data are returned as zero). */
    public List<MonthlyPoint> trend(Long uid, int months) {
        int n = Math.min(Math.max(months, 1), 24);
        YearMonth end = YearMonth.now();
        YearMonth start = end.minusMonths(n - 1L);

        Map<YearMonth, BigDecimal[]> byMonth = new HashMap<>();
        for (Object[] r : txns.monthlyTotals(uid, start.atDay(1), end.atEndOfMonth())) {
            YearMonth ym = YearMonth.of(((Number) r[0]).intValue(), ((Number) r[1]).intValue());
            BigDecimal[] pair = byMonth.computeIfAbsent(ym, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            pair[r[2] == TxnType.INCOME ? 0 : 1] = (BigDecimal) r[3];
        }
        List<MonthlyPoint> out = new ArrayList<>();
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
            BigDecimal[] p = byMonth.getOrDefault(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            out.add(new MonthlyPoint(m.toString(), p[0], p[1]));
        }
        return out;
    }
}
