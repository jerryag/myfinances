package br.com.infotech.myfinances.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailDto {
  private Long id;
  private Long transactionId;
  private OffsetDateTime detailDate;
  private String description;
  private BigDecimal amount;
}
