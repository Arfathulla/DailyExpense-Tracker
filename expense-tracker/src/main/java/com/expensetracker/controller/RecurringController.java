package com.expensetracker.controller;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.service.RecurringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringController {
    private final RecurringService service;

    @GetMapping
    public List<RecurringResponse> list(@AuthenticationPrincipal Long uid) { return service.list(uid); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringResponse create(@AuthenticationPrincipal Long uid, @Valid @RequestBody RecurringRequest r) {
        return service.create(uid, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) { service.delete(uid, id); }
}
