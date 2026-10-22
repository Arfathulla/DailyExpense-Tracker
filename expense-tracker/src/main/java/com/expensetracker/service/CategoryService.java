package com.expensetracker.service;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.entity.Category;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.NotFoundException;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categories;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long uid) {
        return categories.findByUserIdOrderByNameAsc(uid).stream().map(CategoryService::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(Long uid, CategoryRequest r) {
        String name = r.name().trim();
        if (categories.existsByUserIdAndNameIgnoreCaseAndType(uid, name, r.type()))
            throw new BadRequestException("You already have this category");
        Category c = new Category();
        c.setUser(users.getReferenceById(uid));
        c.setName(name);
        c.setType(r.type());
        return toResponse(categories.save(c));
    }

    @Transactional
    public CategoryResponse update(Long uid, Long id, CategoryRequest r) {
        Category c = find(uid, id);
        c.setName(r.name().trim());
        c.setType(r.type());
        return toResponse(categories.save(c));
    }

    @Transactional
    public void delete(Long uid, Long id) {
        categories.delete(find(uid, id));
        categories.flush(); // surfaces a foreign-key violation here (-> 409) instead of at commit
    }

    private Category find(Long uid, Long id) {
        return categories.findByIdAndUserId(id, uid).orElseThrow(() -> new NotFoundException("Category not found"));
    }

    static CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getType());
    }
}
