package com.expensetracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions",
       indexes = @Index(name = "idx_txn_user_date", columnList = "user_id, txn_date"))
@Getter @Setter @NoArgsConstructor
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // plain VARCHAR (not a native MySQL ENUM) so it matches the Flyway schema
    @Column(nullable = false, length = 10)
    private TxnType type;
    @Column(length = 255)
    private String note;
    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
