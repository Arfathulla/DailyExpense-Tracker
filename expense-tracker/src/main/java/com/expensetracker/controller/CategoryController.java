package com.expensetracker.controller;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal Long uid) { return service.list(uid); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@AuthenticationPrincipal Long uid, @Valid @RequestBody CategoryRequest r) {
        return service.create(uid, r);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@AuthenticationPrincipal Long uid, @PathVariable Long id,
                                   @Valid @RequestBody CategoryRequest r) {
        return service.update(uid, id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) { service.delete(uid, id); }
}
