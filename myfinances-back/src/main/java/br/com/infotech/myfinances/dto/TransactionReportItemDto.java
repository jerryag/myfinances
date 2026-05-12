package br.com.infotech.myfinances.dto;

import br.com.infotech.myfinances.domain.TransactionStatus;
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
public class TransactionReportItemDto {
  private Long id;
  private Integer day;
  private Long transactionTypeId;
  private String transactionTypeDescription;
  private String description;
  private BigDecimal amount;
  private TransactionStatus status;
  private String remark;
  private List<TransactionDetailDto> details;
}
