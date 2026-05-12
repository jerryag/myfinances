package br.com.infotech.myfinances.service;

import br.com.infotech.myfinances.domain.Transaction;
import br.com.infotech.myfinances.domain.TransactionMonth;
import br.com.infotech.myfinances.domain.TransactionStatus;
import br.com.infotech.myfinances.domain.TransactionType;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.dto.TransactionDto;
import br.com.infotech.myfinances.dto.TransactionMonthDto;
import br.com.infotech.myfinances.exception.TransactionMonthNotFoundException;
import br.com.infotech.myfinances.repository.TransactionMonthRepository;
import br.com.infotech.myfinances.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import br.com.infotech.myfinances.dto.TransactionMonthReportDto;
import br.com.infotech.myfinances.dto.TransactionReportItemDto;
import br.com.infotech.myfinances.dto.TransactionDetailDto;

@Service
@Transactional(propagation = Propagation.SUPPORTS)
@RequiredArgsConstructor
@Slf4j
public class TransactionMonthService {

  private final TransactionMonthRepository transactionMonthRepository;
  private final TransactionTypeService transactionTypeService;
  private final UserService userService;
  private final TransactionService transactionService;

  /**
   * Recupera o mês de transação atual do usuário ou cria um novo caso não exista.
   *
   * @param month O número do mês (1-12).
   * @param year  O ano.
   * @return O DTO do mês correspondente.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public TransactionMonthDto getOrCreateMonth(Integer month, Integer year) {
    User currentUser = userService.getCurrentUser();

    return transactionMonthRepository.findByUserAndMonthAndYear(currentUser, month, year)
        .map(this::toDto)
        .orElseGet(() -> createMonth(currentUser, month, year));
  }

  private TransactionMonthDto createMonth(User user, Integer month, Integer year) {
    log.debug("Criando a planilha do mês: {}/{}", month, year);
    TransactionMonth newMonth = TransactionMonth.builder().user(user).month(month).year(year).status("OPEN")
        .initialBalance(BigDecimal.ZERO).build();

    TransactionMonth savedMonth = transactionMonthRepository.save(newMonth);

    // Gera automaticamente as transações recorrentes
    List<TransactionType> recurringTypes = transactionTypeService.findRecurringActiveByUser(user);

    for (TransactionType type : recurringTypes) {
      int dayToUse = resolveDay(type, year, month);
      BigDecimal amountToUse = type.getDefaultAmount() != null ? type.getDefaultAmount() : BigDecimal.ZERO;
      String descToUse = type.getDescription() != null ? type.getDescription() : "";

      TransactionDto dto = TransactionDto.builder()
          .transactionTypeId(type.getId())
          .day(dayToUse)
          .description(descToUse)
          .amount(amountToUse)
          .status(TransactionStatus.PENDING)
          .build();
      transactionService.addTransaction(savedMonth.getId(), dto);
    }

    return toDto(savedMonth);
  }

  private int resolveDay(TransactionType type, int year, int month) {
    int day = type.getDefaultDay() != null ? type.getDefaultDay() : 1;
    int maxDay = LocalDate.of(year, month, 1).lengthOfMonth();
    return Math.min(day, maxDay);
  }

  /**
   * Atualiza o saldo inicial de um mês específico.
   *
   * @param id             ID do mês.
   * @param initialBalance O novo saldo inicial.
   * @return O Mês atualizado.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public TransactionMonthDto updateInitialBalance(Long id, BigDecimal initialBalance) {
    log.debug("Iniciando atualização de saldo inicial para o mês ID: {}", id);
    ValidationUtils.notNull(id, "ID do mês obrigatório");
    ValidationUtils.notNull(initialBalance, "Saldo inicial obrigatório");

    TransactionMonth transactionMonth = transactionMonthRepository.findById(id)
        .orElseThrow(() -> new TransactionMonthNotFoundException("Mês não encontrado"));

    transactionMonth.setInitialBalance(initialBalance);
    return toDto(transactionMonthRepository.save(transactionMonth));
  }

  TransactionMonthDto toDto(TransactionMonth month) {
    return TransactionMonthDto.builder()
        .id(month.getId())
        .month(month.getMonth())
        .year(month.getYear())
        .status(month.getStatus())
        .initialBalance(month.getInitialBalance())
        .transactions(transactionService.getSortedTransactions(month))
        .build();
  }

  @Transactional(propagation = Propagation.SUPPORTS)
  public TransactionMonthReportDto getReportData(Integer month, Integer year) {
    User currentUser = userService.getCurrentUser();
    TransactionMonth tMonth = transactionMonthRepository.findByUserAndMonthAndYear(currentUser, month, year)
        .orElseThrow(() -> new TransactionMonthNotFoundException("Mês não encontrado"));

    List<TransactionDto> dtos = transactionService.getSortedTransactions(tMonth);

    List<TransactionReportItemDto> items = dtos.stream().map(dto -> {
      String typeDesc = transactionTypeService.findByIdOrThrow(dto.getTransactionTypeId()).getDescription();
      List<TransactionDetailDto> details = transactionService.getDetail(dto.getId());

      return TransactionReportItemDto.builder()
          .id(dto.getId())
          .day(dto.getDay())
          .transactionTypeId(dto.getTransactionTypeId())
          .transactionTypeDescription(typeDesc)
          .description(dto.getDescription())
          .amount(dto.getAmount())
          .status(dto.getStatus())
          .remark(dto.getRemark())
          .details(details)
          .build();
    }).toList();

    return TransactionMonthReportDto.builder()
        .month(tMonth.getMonth())
        .year(tMonth.getYear())
        .initialBalance(tMonth.getInitialBalance())
        .transactions(items)
        .build();
  }
}
