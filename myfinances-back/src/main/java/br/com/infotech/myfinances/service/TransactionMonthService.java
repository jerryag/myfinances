package br.com.infotech.myfinances.service;

import br.com.infotech.myfinances.domain.*;
import br.com.infotech.myfinances.dto.TransactionDto;
import br.com.infotech.myfinances.dto.TransactionMonthDto;
import br.com.infotech.myfinances.repository.TransactionMonthRepository;
import br.com.infotech.myfinances.repository.TransactionRepository;
import br.com.infotech.myfinances.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Propagation;
import br.com.infotech.myfinances.exception.TransactionMonthNotFoundException;
import br.com.infotech.myfinances.exception.TransactionNotFoundException;
import br.com.infotech.myfinances.exception.TransactionTypeNotFoundException;
import br.com.infotech.myfinances.exception.BusinessException;
import br.com.infotech.myfinances.util.ValidationUtils;
import br.com.infotech.myfinances.domain.TransactionTypeType;

@Service
@Transactional(propagation = Propagation.SUPPORTS)
@RequiredArgsConstructor
@Slf4j
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
    log.debug("Recuperando ou criando a planilha do mês: {}/{}", month, year);
    User currentUser = userService.getCurrentUser();

    return transactionMonthRepository.findByUserAndMonthAndYear(currentUser, month, year)
                                     .map(this::toDto)
                                     .orElseGet(() -> createMonth(currentUser, month, year));
  }

  private TransactionMonthDto createMonth(User user, Integer month, Integer year) {
    TransactionMonth newMonth = TransactionMonth.builder().user(user).month(month).year(year).status("OPEN").initialBalance(BigDecimal.ZERO).build();

    TransactionMonth savedMonth = transactionMonthRepository.save(newMonth);

    // Gera automaticamente as transações recorrentes
    List<TransactionType> recurringTypes = transactionTypeRepository.findAll()
                                                                    .stream()
                                                                    .filter(t -> t.getUser().getId().equals(user.getId())
                                                                        && Boolean.TRUE.equals(t.getRecurring())
                                                                        && t.getStatus() == TransactionTypeStatus.ACTIVE)
                                                                    .collect(Collectors.toList());

    List<Transaction> initialTransactions = new ArrayList<>();
    for (TransactionType type : recurringTypes) {

      // Lógica para Data Recorrente
      // Usa o dia padrão do tipo de transação, ou 1 se não estiver definido
      int dayToUse = type.getDefaultDay() != null ? type.getDefaultDay() : 1;

      // Garante que o dia seja válido para este mês
      int maxDayInNewMonth = LocalDate.of(year, month, 1).lengthOfMonth();
      if (dayToUse > maxDayInNewMonth) {
        dayToUse = maxDayInNewMonth;
      }

      BigDecimal amountToUse = type.getDefaultAmount() != null ? type.getDefaultAmount() : BigDecimal.ZERO;
      String descToUse = type.getDescription() != null ? type.getDescription() : "";

      Transaction transaction = Transaction.builder()
                                           .user(user)
                                           .transactionType(type)
                                           .transactionMonth(savedMonth)
                                           .transactionDate(LocalDate.of(year, month, dayToUse))
                                           .description(descToUse)
                                           .amount(amountToUse)
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
    log.debug("Iniciando atualização de saldo inicial para o mês ID: {}", id);
    ValidationUtils.notNull(id, "ID do mês obrigatório");
    ValidationUtils.notNull(initialBalance, "Saldo inicial obrigatório");

    TransactionMonth transactionMonth = transactionMonthRepository.findById(id).orElseThrow(() -> new TransactionMonthNotFoundException("Mês não encontrado"));

    // Otimização: Validar se o usuário é o dono

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
    log.debug("Adicionando nova transação ao mês ID: {}", monthId);
    ValidationUtils.notNull(monthId, "ID do mês é obrigatório");
    ValidationUtils.notNull(dto, "A transação é obrigatória");
    ValidationUtils.notNull(dto.getTransactionTypeId(), "O tipo de transação é obrigatório");
    ValidationUtils.notNull(dto.getDay(), "O dia é obrigatório");
    ValidationUtils.notNull(dto.getAmount(), "O valor é obrigatório");

    TransactionMonth month = transactionMonthRepository.findById(monthId).orElseThrow(() -> new TransactionMonthNotFoundException("Mês não encontrado"));

    TransactionType type = transactionTypeRepository.findById(dto.getTransactionTypeId())
                                                    .orElseThrow(() -> new TransactionTypeNotFoundException("Tipo de Transação não encontrado"));

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
    log.debug("Atualizando transação ID: {}", transactionId);
    ValidationUtils.notNull(transactionId, "ID da transação é obrigatório");
    ValidationUtils.notNull(dto, "A transação é obrigatória");
    ValidationUtils.notNull(dto.getTransactionTypeId(), "O tipo de transação é obrigatório");
    ValidationUtils.notNull(dto.getDay(), "O dia é obrigatório");
    ValidationUtils.notNull(dto.getAmount(), "O valor é obrigatório");

    Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new TransactionNotFoundException("Transação não encontrada"));

    TransactionType type = transactionTypeRepository.findById(dto.getTransactionTypeId())
                                                    .orElseThrow(() -> new TransactionTypeNotFoundException("Tipo de Transação não encontrado"));

    // Atualiza os campos
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
    log.debug("Iniciando exclusão da transação ID: {}", transactionId);
    ValidationUtils.notNull(transactionId, "ID da transação é obrigatório");
    Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new TransactionNotFoundException("Transação não encontrada"));

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
    log.debug("Buscando último valor registrado para a transação tipo ID: {} e descrição: [{}]", transactionTypeId, description);
    ValidationUtils.notNull(transactionTypeId, "O ID do tipo de transação é obrigatório");
    User currentUser = userService.getCurrentUser();
    TransactionType type = transactionTypeRepository.findById(transactionTypeId)
                                                    .orElseThrow(() -> new TransactionTypeNotFoundException("Tipo de Transação não encontrado"));

    Optional<Transaction> transaction = Optional.empty();

    // 1. Tenta buscar com a Descrição exata
    if (description != null && !description.trim().isEmpty()) {
      transaction = transactionRepository.findFirstByUserAndTransactionTypeAndDescriptionAndAmountGreaterThanOrderByTransactionDateDesc(currentUser, type, description.trim(), BigDecimal.ZERO);
    }

    // 2. Fallback buscando apenas pelo Tipo
    if (transaction.isEmpty()) {
      transaction = transactionRepository.findFirstByUserAndTransactionTypeAndAmountGreaterThanOrderByTransactionDateDesc(currentUser, type, BigDecimal.ZERO);
    }

    return transaction.map(Transaction::getAmount).orElse(BigDecimal.ZERO);
  }
  // TODO: Adicionar métodos de CRUD de Transações aqui (adicionar, atualizar, deletar linha de transação)

  private TransactionMonthDto toDto(TransactionMonth entity) {
    List<Transaction> transactions = transactionRepository.findByTransactionMonthOrderByTransactionDateAsc(entity);

    // Ordenação Customizada: Dia ASC (já vem do banco), depois Tipo (Receita antes de Despesa)
    transactions.sort((t1, t2) -> {
      int dateCompare = t1.getTransactionDate().compareTo(t2.getTransactionDate());
      if (dateCompare != 0)
        return dateCompare;

      // Em dias idênticos, a RECEITA tem prioridade sobre a DESPESA
      boolean t1IsIncome = t1.getTransactionType().getType() == TransactionTypeType.INCOME;
      boolean t2IsIncome = t2.getTransactionType().getType() == TransactionTypeType.INCOME;

      if (t1IsIncome && !t2IsIncome)
        return -1;
      if (!t1IsIncome && t2IsIncome)
        return 1;

      // Desempate 3: Valor (Crescente)
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
