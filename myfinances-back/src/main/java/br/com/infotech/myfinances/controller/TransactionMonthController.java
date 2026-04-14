package br.com.infotech.myfinances.controller;

import br.com.infotech.myfinances.controller.api.ITransactionMonthController;
import br.com.infotech.myfinances.dto.TransactionDto;
import br.com.infotech.myfinances.dto.TransactionMonthDto;
import br.com.infotech.myfinances.service.TransactionMonthService;
import br.com.infotech.myfinances.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class TransactionMonthController implements ITransactionMonthController {

  private final TransactionMonthService transactionMonthService;
  private final TransactionService transactionService;

  @Override
  public ResponseEntity<TransactionMonthDto> getOrCreateMonth(Integer year, Integer month) {
    return ResponseEntity.ok(transactionMonthService.getOrCreateMonth(month, year));
  }

  @Override
  public ResponseEntity<TransactionMonthDto> updateInitialBalance(Long id, BigDecimal initialBalance) {
    return ResponseEntity.ok(transactionMonthService.updateInitialBalance(id, initialBalance));
  }

  @Override
  public ResponseEntity<TransactionMonthDto> addTransaction(Long monthId, TransactionDto dto) {
    return ResponseEntity.ok(transactionService.addTransaction(monthId, dto));
  }

  @Override
  public ResponseEntity<TransactionMonthDto> updateTransaction(Long transactionId, TransactionDto dto) {
    return ResponseEntity.ok(transactionService.updateTransaction(transactionId, dto));
  }

  @Override
  public ResponseEntity<TransactionMonthDto> deleteTransaction(Long transactionId) {
    return ResponseEntity.ok(transactionService.deleteTransaction(transactionId));
  }

  @Override
  public ResponseEntity<BigDecimal> getLastTransactionValue(Long transactionTypeId, String description) {
    return ResponseEntity.ok(transactionService.getLastTransactionValue(transactionTypeId, description));
  }
}
