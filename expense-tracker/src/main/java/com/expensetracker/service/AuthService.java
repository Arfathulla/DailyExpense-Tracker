package com.expensetracker.service;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.TxnType;
import com.expensetracker.entity.User;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final List<String> DEFAULT_EXPENSE =
            List.of("Food", "Rent", "Travel", "Shopping", "Bills", "Health", "Entertainment");
    private static final List<String> DEFAULT_INCOME = List.of("Salary", "Freelance");

    private final UserRepository users;
    private final CategoryRepository categories;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new BadRequestException("This email is already registered");

        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        users.save(u);

        DEFAULT_EXPENSE.forEach(n -> addCategory(u, n, TxnType.EXPENSE));
        DEFAULT_INCOME.forEach(n -> addCategory(u, n, TxnType.INCOME));
        return new AuthResponse(jwt.generate(u.getId(), u.getEmail()), u.getName(), u.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(found -> encoder.matches(r.password(), found.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Incorrect email or password"));
        return new AuthResponse(jwt.generate(u.getId(), u.getEmail()), u.getName(), u.getEmail());
    }

    private void addCategory(User u, String name, TxnType type) {
        Category c = new Category();
        c.setUser(u);
        c.setName(name);
        c.setType(type);
        categories.save(c);
    }
}
