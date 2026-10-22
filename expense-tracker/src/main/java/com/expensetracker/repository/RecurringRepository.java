package com.expensetracker.repository;

import com.expensetracker.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RecurringRepository extends JpaRepository<RecurringTransaction, Long> {
    List<RecurringTransaction> findByUserIdOrderByDayOfMonthAsc(Long userId);
    Optional<RecurringTransaction> findByIdAndUserId(Long id, Long userId);
    List<RecurringTransaction> findByActiveTrue();
}
