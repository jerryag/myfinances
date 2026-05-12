package br.com.infotech.myfinances.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMonthReportDto {
  private Integer month;
  private Integer year;
  private BigDecimal initialBalance;
  private List<TransactionReportItemDto> transactions;
}
