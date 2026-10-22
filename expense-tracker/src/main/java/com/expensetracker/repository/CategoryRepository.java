package com.expensetracker.repository;

import com.expensetracker.entity.Category;
import com.expensetracker.entity.TxnType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrderByNameAsc(Long userId);
    Optional<Category> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndNameIgnoreCaseAndType(Long userId, String name, TxnType type);
}
