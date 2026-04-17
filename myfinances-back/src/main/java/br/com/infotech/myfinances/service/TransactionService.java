package br.com.infotech.myfinances.service;

import br.com.infotech.myfinances.domain.Transaction;
import br.com.infotech.myfinances.domain.TransactionMonth;
import br.com.infotech.myfinances.domain.TransactionType;
import br.com.infotech.myfinances.domain.TransactionTypeType;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.dto.TransactionDto;
import br.com.infotech.myfinances.exception.TransactionMonthNotFoundException;
import br.com.infotech.myfinances.exception.TransactionNotFoundException;
import br.com.infotech.myfinances.exception.TransactionTypeNotFoundException;
import br.com.infotech.myfinances.exception.ValidationException;
import br.com.infotech.myfinances.repository.TransactionMonthRepository;
import br.com.infotech.myfinances.repository.TransactionRepository;
import br.com.infotech.myfinances.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(propagation = Propagation.SUPPORTS)
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final TransactionTypeService transactionTypeService;
  private final TransactionMonthRepository transactionMonthRepository;
  private final UserService userService;

  /**
   * Adiciona uma nova transação a um mês existente.
   *
   * @param monthId ID do mês ao qual a transação será adicionada.
   * @param dto     Dados da nova transação.
   * @return A transação adicionada.
   * @throws ValidationException               se o ID do mês, o DTO, o tipo de
   *                                           transação, o dia ou o valor forem
   *                                           nulos.
   * @throws TransactionMonthNotFoundException se o mês não for encontrado.
   * @throws TransactionTypeNotFoundException  se o tipo de transação não for
   *                                           encontrado.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public TransactionDto addTransaction(Long monthId, TransactionDto dto) {
    log.debug("Adicionando nova transação ao mês ID: {}", monthId);
    ValidationUtils.notNull(monthId, "ID do mês é obrigatório");
    ValidationUtils.notNull(dto, "A transação é obrigatória");
    ValidationUtils.notNull(dto.getTransactionTypeId(), "O tipo de transação é obrigatório");
    ValidationUtils.notNull(dto.getDay(), "O dia é obrigatório");
    ValidationUtils.notNull(dto.getAmount(), "O valor é obrigatório");

    TransactionMonth month = transactionMonthRepository.findById(monthId)
        .orElseThrow(() -> new TransactionMonthNotFoundException("Mês não encontrado"));

    TransactionType type = transactionTypeService.findByIdOrThrow(dto.getTransactionTypeId());

    return toDto(transactionRepository.save(buildTransaction(month, type, dto)));
  }

  /**
   * Atualiza uma transação existente com os dados informados.
   *
   * @param transactionId ID da transação a ser atualizada.
   * @param dto           Os novos dados da transação.
   * @return A transação atualizada.
   * @throws ValidationException              se o ID da transação, o DTO, o tipo
   *                                          de transação, o dia ou o valor forem
   *                                          nulos.
   * @throws TransactionNotFoundException     se a transação não for encontrada
   *                                          pelo ID informado.
   * @throws TransactionTypeNotFoundException se o tipo de transação não for
   *                                          encontrado.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public TransactionDto updateTransaction(Long transactionId, TransactionDto dto) {
    log.debug("Atualizando transação ID: {}", transactionId);
    ValidationUtils.notNull(transactionId, "ID da transação é obrigatório");
    ValidationUtils.notNull(dto, "A transação é obrigatória");
    ValidationUtils.notNull(dto.getTransactionTypeId(), "O tipo de transação é obrigatório");
    ValidationUtils.notNull(dto.getDay(), "O dia é obrigatório");
    ValidationUtils.notNull(dto.getAmount(), "O valor é obrigatório");

    Transaction transaction = transactionRepository.findById(transactionId)
        .orElseThrow(() -> new TransactionNotFoundException("Transação não encontrada"));

    TransactionType type = transactionTypeService.findByIdOrThrow(dto.getTransactionTypeId());

    applyUpdate(transaction, type, dto);

    return toDto(transactionRepository.save(transaction));
  }

  private Transaction buildTransaction(TransactionMonth month, TransactionType type, TransactionDto dto) {
    return Transaction.builder()
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
  }

  private void applyUpdate(Transaction transaction, TransactionType type, TransactionDto dto) {
    int year = transaction.getTransactionMonth().getYear();
    int month = transaction.getTransactionMonth().getMonth();

    transaction.setTransactionType(type);
    transaction.setTransactionDate(LocalDate.of(year, month, dto.getDay()));
    transaction.setDescription(dto.getDescription());
    transaction.setAmount(dto.getAmount());
    transaction.setStatus(dto.getStatus());
    transaction.setRemark(dto.getRemark());
    transaction.setIconName(dto.getIconName());
  }

  /**
   * Remove uma transação através de seu ID.
   *
   * @param transactionId O ID da transação a ser removida.
   * @throws ValidationException          se o ID da transação for nulo.
   * @throws TransactionNotFoundException se a transação não for encontrada.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void deleteTransaction(Long transactionId) {
    log.debug("Iniciando exclusão da transação ID: {}", transactionId);
    ValidationUtils.notNull(transactionId, "ID da transação é obrigatório");

    Transaction transaction = transactionRepository.findById(transactionId)
        .orElseThrow(() -> new TransactionNotFoundException("Transação não encontrada"));

    transactionRepository.delete(transaction);
  }

  /**
   * Recupera o último valor registrado de uma transação de determinado tipo e
   * descrição.
   * Caso não seja informada descrição, procura apenas pelo tipo.
   *
   * @param transactionTypeId O ID do tipo de transação.
   * @param description       A descrição da transação (opcional).
   * @return O valor da última transação encontrada, ou {@link BigDecimal#ZERO} se
   *         nenhuma for encontrada.
   * @throws ValidationException              se o ID do tipo de transação for
   *                                          nulo.
   * @throws TransactionTypeNotFoundException se o tipo de transação não for
   *                                          encontrado.
   */
  public BigDecimal getLastTransactionValue(Long transactionTypeId, String description) {
    log.debug("Buscando último valor registrado para a transação tipo ID: {} e descrição: [{}]", transactionTypeId,
        description);
    ValidationUtils.notNull(transactionTypeId, "O ID do tipo de transação é obrigatório");

    User currentUser = userService.getCurrentUser();
    TransactionType type = transactionTypeService.findByIdOrThrow(transactionTypeId);

    Optional<Transaction> transaction = findLastTransaction(currentUser, type, description);
    return transaction.map(Transaction::getAmount).orElse(BigDecimal.ZERO);
  }

  private Optional<Transaction> findLastTransaction(User user, TransactionType type, String description) {
    if (description != null && !description.trim().isEmpty()) {
      Optional<Transaction> byDescription = transactionRepository
          .findFirstByUserAndTransactionTypeAndDescriptionAndAmountGreaterThanOrderByTransactionDateDesc(
              user, type, description.trim(), BigDecimal.ZERO);
      if (byDescription.isPresent()) {
        return byDescription;
      }
    }
    return transactionRepository
        .findFirstByUserAndTransactionTypeAndAmountGreaterThanOrderByTransactionDateDesc(user, type, BigDecimal.ZERO);
  }

  /**
   * Retorna as transações de um mês ordenadas por data ascendente, com receitas
   * antes de despesas no mesmo dia
   * e, em caso de empate, por valor crescente.
   *
   * @param month O mês de referência.
   * @return Lista ordenada de {@link TransactionDto}.
   */
  public List<TransactionDto> getSortedTransactions(TransactionMonth month) {
    List<Transaction> transactions = transactionRepository.findByTransactionMonthOrderByTransactionDateAsc(month);
    transactions.sort(this::compareTransactions);
    return transactions.stream().map(this::toDto).toList();
  }

  public TransactionDto toDto(Transaction t) {
    if (t == null) {
      return null;
    }
    return TransactionDto.builder()
        .id(t.getId())
        .day(t.getTransactionDate().getDayOfMonth())
        .transactionTypeId(t.getTransactionType().getId())
        .description(t.getDescription())
        .amount(t.getAmount())
        .status(t.getStatus())
        .remark(t.getRemark())
        .iconName(t.getIconName())
        .build();
  }

  private int compareTransactions(Transaction t1, Transaction t2) {
    int dateCompare = t1.getTransactionDate().compareTo(t2.getTransactionDate());
    if (dateCompare != 0) {
      return dateCompare;
    }
    // Em dias idênticos, a RECEITA tem prioridade sobre a DESPESA
    boolean t1IsIncome = t1.getTransactionType().getType() == TransactionTypeType.INCOME;
    boolean t2IsIncome = t2.getTransactionType().getType() == TransactionTypeType.INCOME;
    if (t1IsIncome && !t2IsIncome) {
      return -1;
    }
    if (!t1IsIncome && t2IsIncome) {
      return 1;
    }
    // Desempate: Valor crescente
    return t1.getAmount().compareTo(t2.getAmount());
  }
}
