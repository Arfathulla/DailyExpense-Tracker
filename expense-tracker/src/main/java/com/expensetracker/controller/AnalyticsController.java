package com.expensetracker.controller;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.entity.TxnType;
import com.expensetracker.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService service;

    private static YearMonth parse(String month) {
        return month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
    }

    @GetMapping("/summary")
    public Summary summary(@AuthenticationPrincipal Long uid, @RequestParam(required = false) String month) {
        return service.summary(uid, parse(month));
    }

    @GetMapping("/by-category")
    public List<CategoryTotal> byCategory(@AuthenticationPrincipal Long uid,
                                          @RequestParam(required = false) String month,
                                          @RequestParam(defaultValue = "EXPENSE") TxnType type) {
        return service.byCategory(uid, parse(month), type);
    }

    @GetMapping("/monthly-trend")
    public List<MonthlyPoint> trend(@AuthenticationPrincipal Long uid, @RequestParam(defaultValue = "6") int months) {
        return service.trend(uid, months);
    }
}
