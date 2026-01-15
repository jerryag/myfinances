package br.com.infotech.myfinances.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "transaction_type_id", nullable = false)
    private TransactionType transactionType;

    @ManyToOne
    @JoinColumn(name = "transaction_month_id", nullable = false)
    private TransactionMonth transactionMonth;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = true, length = 50)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(length = 100)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
