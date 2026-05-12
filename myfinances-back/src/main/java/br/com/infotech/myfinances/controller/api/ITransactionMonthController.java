package br.com.infotech.myfinances.controller.api;

import br.com.infotech.myfinances.dto.TransactionDto;
import br.com.infotech.myfinances.dto.TransactionMonthDto;
import br.com.infotech.myfinances.dto.TransactionDetailDto;
import br.com.infotech.myfinances.dto.TransactionMonthReportDto;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Tag(name = "Transaction Months", description = "Gerenciamento de Transações Mensais (Planilha)")
@RequestMapping("/transaction-months")
public interface ITransactionMonthController {

  @GetMapping("/{year}/{month}")
  @Operation(summary = "Obter ou criar mês", description = "Retorna os dados do mês. Se não existir, cria e gera transações recorrentes.")
  ResponseEntity<TransactionMonthDto> getOrCreateMonth(@PathVariable("year") Integer year, @PathVariable("month") Integer month);

  @PatchMapping("/{id}/initial-balance")
  @Operation(summary = "Atualizar saldo inicial", description = "Atualiza o saldo inicial do mês.")
  ResponseEntity<TransactionMonthDto> updateInitialBalance(@PathVariable("id") Long id, @RequestBody BigDecimal initialBalance);

  @PostMapping("/{monthId}/transactions")
  @Operation(summary = "Adicionar transação", description = "Adiciona uma nova linha de transação ao mês.")
  ResponseEntity<TransactionDto> addTransaction(@PathVariable("monthId") Long monthId, @RequestBody TransactionDto dto);

  @PutMapping("/transactions/{transactionId}")
  @Operation(summary = "Atualizar transação", description = "Atualiza uma linha de transação existente.")
  ResponseEntity<TransactionDto> updateTransaction(@PathVariable("transactionId") Long transactionId, @RequestBody TransactionDto dto);

  @DeleteMapping("/transactions/{transactionId}")
  @Operation(summary = "Excluir transação", description = "Remove uma linha de transação.")
  ResponseEntity<Void> deleteTransaction(@PathVariable("transactionId") Long transactionId);

  @GetMapping("/last-value")
  @Operation(summary = "Obter último valor", description = "Busca o último valor utilizado para um Tipo e Descrição.")
  ResponseEntity<BigDecimal> getLastTransactionValue(@RequestParam("transactionTypeId") Long transactionTypeId,
                                                     @RequestParam(value = "description", required = false) String description);

  @GetMapping("/transactions/{transactionId}/details")
  @Operation(summary = "Listar detalhes", description = "Lista os detalhes de uma transação específica.")
  ResponseEntity<List<TransactionDetailDto>> getTransactionDetails(@PathVariable("transactionId") Long transactionId);

  @PostMapping("/transactions/{transactionId}/details")
  @Operation(summary = "Salvar detalhe", description = "Cria ou atualiza um detalhe para uma transação.")
  ResponseEntity<TransactionDetailDto> saveTransactionDetail(@PathVariable("transactionId") Long transactionId, @RequestBody TransactionDetailDto dto);

  @DeleteMapping("/transactions/details/{detailId}")
  @Operation(summary = "Excluir detalhe", description = "Remove um detalhe de uma transação.")
  ResponseEntity<Void> deleteTransactionDetail(@PathVariable("detailId") Long detailId);

  @GetMapping("/{year}/{month}/report")
  @Operation(summary = "Dados do relatório", description = "Retorna os dados do mês com todos os detalhes das transações preenchidos, otimizado para o relatório.")
  ResponseEntity<TransactionMonthReportDto> getReportData(@PathVariable("year") Integer year, @PathVariable("month") Integer month);
}
