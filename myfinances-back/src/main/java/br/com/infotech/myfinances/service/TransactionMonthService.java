package br.com.infotech.myfinances.service;

import br.com.infotech.myfinances.domain.*;
import br.com.infotech.myfinances.dto.TransactionDto;
import br.com.infotech.myfinances.dto.TransactionMonthDto;
import br.com.infotech.myfinances.repository.TransactionMonthRepository;
import br.com.infotech.myfinances.repository.TransactionRepository;
import br.com.infotech.myfinances.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Propagation;
import br.com.infotech.myfinances.exception.BusinessException;
import br.com.infotech.myfinances.util.ValidationUtils;
import br.com.infotech.myfinances.domain.TransactionTypeType;

@Service
@Transactional(propagation = Propagation.SUPPORTS)
@RequiredArgsConstructor
public class TransactionMonthService {

  private final TransactionMonthRepository transactionMonthRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionTypeRepository transactionTypeRepository;
  private final UserService userService;

  /**
   * Recupera o mês de transação atual do usuário ou cria um novo caso não exista.
   *
   * @param month O número do mês (1-12).
   * @param year O ano.
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
    TransactionMonth newMonth = TransactionMonth.builder().user(user).month(month).year(year).status("OPEN").initialBalance(BigDecimal.ZERO).build();

    TransactionMonth savedMonth = transactionMonthRepository.save(newMonth);

    // Auto-generate recurring transactions
    List<TransactionType> recurringTypes = transactionTypeRepository.findAll()
                                                                    .stream()
                                                                    .filter(t -> t.getUser().getId().equals(user.getId())
                                                                        && Boolean.TRUE.equals(t.getRecurring())
                                                                        && t.getStatus() == TransactionTypeStatus.ACTIVE)
                                                                    .collect(Collectors.toList());

    List<Transaction> initialTransactions = new ArrayList<>();
    for (TransactionType type : recurringTypes) {

      // Logic for Recurring Date
      // Default to type's default day, or 1 if not set
      int dayToUse = type.getDefaultDay() != null ? type.getDefaultDay() : 1;

      // Ensure day is valid for this month
      int maxDayInNewMonth = LocalDate.of(year, month, 1).lengthOfMonth();
      if (dayToUse > maxDayInNewMonth) {
        dayToUse = maxDayInNewMonth;
      }

      Transaction transaction = Transaction.builder()
                                           .user(user)
                                           .transactionType(type)
                                           .transactionMonth(savedMonth)
                                           .transactionDate(LocalDate.of(year, month, dayToUse))
                                           .description("")
                                           .amount(BigDecimal.ZERO)
                                           .status(TransactionStatus.PENDING)
                                           .createdAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC))
                                           .build();
      initialTransactions.add(transactionRepository.save(transaction));
    }

    return toDto(savedMonth, initialTransactions);
  }

  /**
   * Atualiza o saldo inicial de um mês específico.
   *
   * @param id ID do mês.
   * @param initialBalance O novo saldo inicial.
   * @return O Mês atualizado.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public TransactionMonthDto updateInitialBalance(Long id, BigDecimal initialBalance) {
    ValidationUtils.notNull(id, "ID do mês obrigatório");
    ValidationUtils.notNull(initialBalance, "Saldo inicial obrigatório");

    TransactionMonth transactionMonth = transactionMonthRepository.findById(id).orElseThrow(() -> new BusinessException("Mês não encontrado"));

    // Optimize: Check user ownership

    transactionMonth.setInitialBalance(initialBalance);
    return toDto(transactionMonthRepository.save(transactionMonth));
  }

  /**
   * Adiciona uma nova transação a um mês existente.
   *
   * @param monthId ID do mês.
   * @param dto Dados da nova transação.
   * @return O mês atualizado listando as transações.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public TransactionMonthDto addTransaction(Long monthId, TransactionDto dto) {
    ValidationUtils.notNull(monthId, "ID do mês é obrigatório");
    ValidationUtils.notNull(dto, "A transação é obrigatória");
    ValidationUtils.notNull(dto.getTransactionTypeId(), "O tipo de transação é obrigatório");
    ValidationUtils.notNull(dto.getDay(), "O dia é obrigatório");
    ValidationUtils.notNull(dto.getAmount(), "O valor é obrigatório");

    TransactionMonth month = transactionMonthRepository.findById(monthId).orElseThrow(() -> new BusinessException("Mês não encontrado"));

    TransactionType type = transactionTypeRepository.findById(dto.getTransactionTypeId())
                                                    .orElseThrow(() -> new BusinessException("Tipo de Transação não encontrado"));

    Transaction transaction = Transaction.builder()
                                         .user(month.getUser())
                                         .transactionMonth(month)
                                         .transactionType(type)
                                         .transactionDate(LocalDate.of(month.getYear(), month.getMonth(), dto.getDay()))
                                         .description(dto.getDescription())
                                         .amount(dto.getAmount())
                                         .status(dto.getStatus())
                                         .remark(dto.getRemark())
                                         .iconName(dto.getIconName())
                                         .build();

    transactionRepository.save(transaction);
    return toDto(month);
  }

  /**
   * Atualiza uma transação existente.
   *
   * @param transactionId ID da transação a ser atualizada.
   * @param dto Os novos dados da transação.
   * @return O mês atualizado.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public TransactionMonthDto updateTransaction(Long transactionId, TransactionDto dto) {
    ValidationUtils.notNull(transactionId, "ID da transação é obrigatório");
    ValidationUtils.notNull(dto, "A transação é obrigatória");
    ValidationUtils.notNull(dto.getTransactionTypeId(), "O tipo de transação é obrigatório");
    ValidationUtils.notNull(dto.getDay(), "O dia é obrigatório");
    ValidationUtils.notNull(dto.getAmount(), "O valor é obrigatório");

    Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new BusinessException("Transação não encontrada"));

    TransactionType type = transactionTypeRepository.findById(dto.getTransactionTypeId())
                                                    .orElseThrow(() -> new BusinessException("Tipo de Transação não encontrado"));

    // Update fields
    transaction.setTransactionType(type);
    transaction.setTransactionDate(LocalDate.of(transaction.getTransactionMonth().getYear(), transaction.getTransactionMonth()
                                                                                                        .getMonth(), dto.getDay()));
    transaction.setDescription(dto.getDescription());
    transaction.setAmount(dto.getAmount());
    transaction.setStatus(dto.getStatus());
    transaction.setRemark(dto.getRemark());
    transaction.setIconName(dto.getIconName());

    transactionRepository.save(transaction);
    return toDto(transaction.getTransactionMonth());
  }

  /**
   * Remove uma transação através de seu ID.
   *
   * @param transactionId O ID da transação.
   * @return O mês atualizado com a transação removida.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public TransactionMonthDto deleteTransaction(Long transactionId) {
    ValidationUtils.notNull(transactionId, "ID da transação é obrigatório");
    Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new BusinessException("Transação não encontrada"));

    TransactionMonth month = transaction.getTransactionMonth();
    transactionRepository.delete(transaction);

    return toDto(month);
  }

  /**
   * Recupera o último valor de transação lançado de determinado tipo e descrição. Caso não seja informada descrição, procura apenas pelo tipo.
   *
   * @param transactionTypeId O ID do tipo de transação.
   * @param description A descrição da transação (opcional).
   * @return O valor da última transação, ou ZERO.
   */
  public BigDecimal getLastTransactionValue(Long transactionTypeId, String description) {
    ValidationUtils.notNull(transactionTypeId, "O ID do tipo de transação é obrigatório");
    User currentUser = userService.getCurrentUser();
    TransactionType type = transactionTypeRepository.findById(transactionTypeId)
                                                    .orElseThrow(() -> new BusinessException("Tipo de Transação não encontrado"));

    Optional<Transaction> transaction = Optional.empty();

    // 1. Try with Exact Description
    if (description != null && !description.trim().isEmpty()) {
      transaction = transactionRepository.findFirstByUserAndTransactionTypeAndDescriptionAndAmountGreaterThanOrderByTransactionDateDesc(currentUser, type, description.trim(), BigDecimal.ZERO);
    }

    // 2. Fallback to just Type
    if (transaction.isEmpty()) {
      transaction = transactionRepository.findFirstByUserAndTransactionTypeAndAmountGreaterThanOrderByTransactionDateDesc(currentUser, type, BigDecimal.ZERO);
    }

    return transaction.map(Transaction::getAmount).orElse(BigDecimal.ZERO);
  }
  // TODO: Add Transaction CRUD methods here (add, update, delete transaction
  // line)

  private TransactionMonthDto toDto(TransactionMonth entity) {
    List<Transaction> transactions = transactionRepository.findByTransactionMonthOrderByTransactionDateAsc(entity);

    // Custom Sort: Day ASC (already from DB), then Type (Income before Expense)
    transactions.sort((t1, t2) -> {
      int dateCompare = t1.getTransactionDate().compareTo(t2.getTransactionDate());
      if (dateCompare != 0)
        return dateCompare;

      // If same date, INCOME comes before EXPENSE
      boolean t1IsIncome = t1.getTransactionType().getType() == TransactionTypeType.INCOME;
      boolean t2IsIncome = t2.getTransactionType().getType() == TransactionTypeType.INCOME;

      if (t1IsIncome && !t2IsIncome)
        return -1;
      if (!t1IsIncome && t2IsIncome)
        return 1;

      // Tie-breaker 3: Amount (ASC)
      return t1.getAmount().compareTo(t2.getAmount());
    });

    return toDto(entity, transactions);
  }

  private TransactionMonthDto toDto(TransactionMonth entity, List<Transaction> transactions) {
    List<TransactionDto> transactionDtos = transactions.stream()
                                                       .map(t -> TransactionDto.builder()
                                                                               .id(t.getId())
                                                                               .day(t.getTransactionDate().getDayOfMonth())
                                                                               .transactionTypeId(t.getTransactionType().getId())
                                                                               .description(t.getDescription())
                                                                               .amount(t.getAmount())
                                                                               .amount(t.getAmount())
                                                                               .status(t.getStatus())
                                                                               .remark(t.getRemark())
                                                                               .iconName(t.getIconName())
                                                                               .build())
                                                       .collect(Collectors.toList());

    return TransactionMonthDto.builder()
                              .id(entity.getId())
                              .month(entity.getMonth())
                              .year(entity.getYear())
                              .status(entity.getStatus())
                              .initialBalance(entity.getInitialBalance())
                              .transactions(transactionDtos)
                              .build();
  }
}
