package br.com.infotech.myfinances.domain;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
  @Min(1)
  @Max(31)
  private Integer defaultDay;

  @Column(name = "default_amount")
  private BigDecimal defaultAmount;

  @Column(name = "icon_name", length = 50)
  private String iconName;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TransactionTypeStatus status;
}
