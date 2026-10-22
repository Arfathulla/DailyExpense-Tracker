package com.expensetracker.controller;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService service;

    @GetMapping
    public List<BudgetResponse> list(@AuthenticationPrincipal Long uid, @RequestParam(required = false) String month) {
        return service.list(uid, month == null ? YearMonth.now().toString() : month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse save(@AuthenticationPrincipal Long uid, @Valid @RequestBody BudgetRequest r) {
        return service.upsert(uid, r);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(@AuthenticationPrincipal Long uid, @PathVariable Long id,
                                 @Valid @RequestBody BudgetRequest r) {
        return service.update(uid, id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) { service.delete(uid, id); }
}
