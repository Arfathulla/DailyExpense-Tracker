package com.expensetracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;

@Entity
@Table(name = "recurring_transactions")
@Getter @Setter @NoArgsConstructor
public class RecurringTransaction {
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
    @Column(name = "day_of_month", nullable = false)
    private int dayOfMonth;
    @Column(nullable = false)
    private boolean active = true;
    /** Last month (yyyy-MM) this rule generated a transaction; prevents duplicates. */
    @Column(name = "last_run_month", length = 7)
    private String lastRunMonth;
}
