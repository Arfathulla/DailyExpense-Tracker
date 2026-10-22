package com.expensetracker.repository;

import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TxnType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t " +
           "where t.user.id = :uid and t.type = :type and t.txnDate between :from and :to")
    BigDecimal sumByType(@Param("uid") Long uid, @Param("type") TxnType type,
                         @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select t.category.name, sum(t.amount) from Transaction t " +
           "where t.user.id = :uid and t.type = :type and t.txnDate between :from and :to " +
           "group by t.category.name order by sum(t.amount) desc")
    List<Object[]> sumByCategory(@Param("uid") Long uid, @Param("type") TxnType type,
                                 @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t " +
           "where t.user.id = :uid and t.category.id = :cid and t.type = :type " +
           "and t.txnDate between :from and :to")
    BigDecimal sumForCategory(@Param("uid") Long uid, @Param("cid") Long cid, @Param("type") TxnType type,
                              @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select year(t.txnDate), month(t.txnDate), t.type, sum(t.amount) from Transaction t " +
           "where t.user.id = :uid and t.txnDate between :from and :to " +
           "group by year(t.txnDate), month(t.txnDate), t.type")
    List<Object[]> monthlyTotals(@Param("uid") Long uid, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
