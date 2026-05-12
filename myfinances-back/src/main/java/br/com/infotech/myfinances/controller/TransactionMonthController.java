package br.com.infotech.myfinances.controller;

import br.com.infotech.myfinances.controller.api.ITransactionMonthController;
import br.com.infotech.myfinances.dto.TransactionDto;
import br.com.infotech.myfinances.dto.TransactionMonthDto;
import br.com.infotech.myfinances.dto.TransactionDetailDto;
import br.com.infotech.myfinances.dto.TransactionMonthReportDto;
import br.com.infotech.myfinances.service.TransactionMonthService;
import br.com.infotech.myfinances.service.TransactionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
  public ResponseEntity<TransactionDto> addTransaction(Long monthId, TransactionDto dto) {
    return ResponseEntity.ok(transactionService.addTransaction(monthId, dto));
  }

  @Override
  public ResponseEntity<TransactionDto> updateTransaction(Long transactionId, TransactionDto dto) {
    return ResponseEntity.ok(transactionService.updateTransaction(transactionId, dto));
  }

  @Override
  public ResponseEntity<Void> deleteTransaction(Long transactionId) {
    transactionService.deleteTransaction(transactionId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<BigDecimal> getLastTransactionValue(Long transactionTypeId, String description) {
    return ResponseEntity.ok(transactionService.getLastTransactionValue(transactionTypeId, description));
  }

  @Override
  public ResponseEntity<List<TransactionDetailDto>> getTransactionDetails(Long transactionId) {
    return ResponseEntity.ok(transactionService.getDetail(transactionId));
  }

  @Override
  public ResponseEntity<TransactionDetailDto> saveTransactionDetail(Long transactionId, TransactionDetailDto dto) {
    return ResponseEntity.ok(transactionService.saveDetail(transactionId, dto));
  }

  @Override
  public ResponseEntity<Void> deleteTransactionDetail(Long detailId) {
    transactionService.removeDetail(detailId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{year}/{month}/report")
  public ResponseEntity<TransactionMonthReportDto> getReportData(@PathVariable Integer year, @PathVariable Integer month) {
    return ResponseEntity.ok(transactionMonthService.getReportData(month, year));
  }
}
