package br.com.infotech.myfinances.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transaction_type")
public class TransactionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionTypeType type;

    @NotBlank
    @Column(nullable = false, length = 40)
    private String description;

    @NotNull
    @Column(nullable = false)
    private Boolean recurring;
    @Column(name = "default_day")
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(31)
    private Integer defaultDay;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionTypeStatus status;
}
