package com.expensetracker.dto;

import com.expensetracker.entity.TxnType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 6, max = 100) String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record AuthResponse(String token, String name, String email) {}

    public record CategoryRequest(@NotBlank @Size(max = 50) String name, @NotNull TxnType type) {}

    public record CategoryResponse(Long id, String name, TxnType type) {}

    public record TransactionRequest(
            @NotNull Long categoryId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull TxnType type,
            @Size(max = 255) String note,
            @NotNull LocalDate txnDate) {}

    public record TransactionResponse(Long id, Long categoryId, String categoryName,
                                      BigDecimal amount, TxnType type, String note, LocalDate txnDate) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record BudgetRequest(
            @NotNull Long categoryId,
            @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "month must be yyyy-MM") String month,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal limitAmount) {}

    public record BudgetResponse(Long id, Long categoryId, String categoryName, String month,
                                 BigDecimal limitAmount, BigDecimal spent, double percentUsed, String status) {}

    public record Summary(BigDecimal income, BigDecimal expense, BigDecimal balance) {}

    public record CategoryTotal(String category, BigDecimal total) {}

    public record MonthlyPoint(String month, BigDecimal income, BigDecimal expense) {}

    public record RecurringRequest(
            @NotNull Long categoryId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull TxnType type,
            @Size(max = 255) String note,
            @NotNull @Min(1) @Max(31) Integer dayOfMonth) {}

    public record RecurringResponse(Long id, Long categoryId, String categoryName, BigDecimal amount,
                                    TxnType type, String note, int dayOfMonth, boolean active) {}
}
