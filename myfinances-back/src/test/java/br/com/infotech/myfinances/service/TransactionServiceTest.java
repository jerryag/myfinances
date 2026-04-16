package br.com.infotech.myfinances.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import br.com.infotech.myfinances.domain.Transaction;
import br.com.infotech.myfinances.domain.TransactionMonth;
import br.com.infotech.myfinances.domain.TransactionStatus;
import br.com.infotech.myfinances.domain.TransactionType;
import br.com.infotech.myfinances.domain.TransactionTypeType;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.dto.TransactionDto;
import br.com.infotech.myfinances.dto.TransactionMonthDto;
import br.com.infotech.myfinances.exception.TransactionMonthNotFoundException;
import br.com.infotech.myfinances.exception.TransactionNotFoundException;
import br.com.infotech.myfinances.exception.TransactionTypeNotFoundException;
import br.com.infotech.myfinances.exception.ValidationException;
import br.com.infotech.myfinances.repository.TransactionMonthRepository;
import br.com.infotech.myfinances.repository.TransactionRepository;
import br.com.infotech.myfinances.repository.TransactionTypeRepository;

@ExtendWith(SpringExtension.class)
class TransactionServiceTest {

  @InjectMocks
  private TransactionService transactionService;

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private TransactionTypeRepository transactionTypeRepository;

  @Mock
  private TransactionMonthRepository transactionMonthRepository;

  @Mock
  private TransactionMonthService transactionMonthService;

  @Mock
  private UserService userService;

  @Captor
  private ArgumentCaptor<Transaction> transactionCaptor;

  private User mockUser;
  private TransactionMonth mockMonth;
  private TransactionType mockTypeIncome;
  private TransactionType mockTypeExpense;

  @BeforeEach
  void setUp() {
    mockUser = User.builder().id(1L).login("user1").build();
    mockMonth = TransactionMonth.builder().id(10L).user(mockUser).year(2023).month(10).build();
    mockTypeIncome = TransactionType.builder().id(100L).type(TransactionTypeType.INCOME).build();
    mockTypeExpense = TransactionType.builder().id(200L).type(TransactionTypeType.EXPENSE).build();

    lenient().when(userService.getCurrentUser()).thenReturn(mockUser);
  }

  private TransactionDto createValidDto(Long typeId) {
    return TransactionDto.builder()
        .transactionTypeId(typeId)
        .day(15)
        .amount(new BigDecimal("150.00"))
        .description("Test trans")
        .status(TransactionStatus.COMPLETED)
        .build();
  }

  // --- addTransaction ---
  @Test
  void testAddTransaction_Success() {
    TransactionDto dto = createValidDto(100L);
    when(transactionMonthRepository.findById(10L)).thenReturn(Optional.of(mockMonth));
    when(transactionTypeRepository.findById(100L)).thenReturn(Optional.of(mockTypeIncome));
    when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
    TransactionMonthDto expectedDto = TransactionMonthDto.builder().build();
    when(transactionMonthService.toDto(mockMonth)).thenReturn(expectedDto);

    TransactionMonthDto result = transactionService.addTransaction(10L, dto);

    assertNotNull(result);
    verify(transactionRepository).save(transactionCaptor.capture());
    Transaction captured = transactionCaptor.getValue();
    assertEquals(mockUser, captured.getUser());
    assertEquals(mockMonth, captured.getTransactionMonth());
    assertEquals(mockTypeIncome, captured.getTransactionType());
    assertEquals(LocalDate.of(2023, 10, 15), captured.getTransactionDate());
    assertEquals(new BigDecimal("150.00"), captured.getAmount());
  }

  @Test
  void testAddTransaction_ValidationFailed() {
    assertThrows(ValidationException.class, () -> transactionService.addTransaction(null, createValidDto(100L)));
    assertThrows(ValidationException.class, () -> transactionService.addTransaction(10L, null));
    TransactionDto invalidDto = createValidDto(100L);
    invalidDto.setDay(null);
    assertThrows(ValidationException.class, () -> transactionService.addTransaction(10L, invalidDto));
  }

  @Test
  void testAddTransaction_MonthNotFound() {
    when(transactionMonthRepository.findById(10L)).thenReturn(Optional.empty());
    assertThrows(TransactionMonthNotFoundException.class,
        () -> transactionService.addTransaction(10L, createValidDto(100L)));
  }

  @Test
  void testAddTransaction_TypeNotFound() {
    when(transactionMonthRepository.findById(10L)).thenReturn(Optional.of(mockMonth));
    when(transactionTypeRepository.findById(100L)).thenReturn(Optional.empty());
    assertThrows(TransactionTypeNotFoundException.class,
        () -> transactionService.addTransaction(10L, createValidDto(100L)));
  }

  // --- updateTransaction ---
  @Test
  void testUpdateTransaction_Success() {
    TransactionDto dto = createValidDto(100L);
    dto.setDay(20);
    dto.setDescription("Updated desc");

    Transaction existingTrans = Transaction.builder()
        .id(50L)
        .transactionMonth(mockMonth)
        .transactionType(mockTypeExpense)
        .transactionDate(LocalDate.of(2023, 10, 5))
        .build();

    when(transactionRepository.findById(50L)).thenReturn(Optional.of(existingTrans));
    when(transactionTypeRepository.findById(100L)).thenReturn(Optional.of(mockTypeIncome));
    TransactionMonthDto expectedDto = TransactionMonthDto.builder().build();
    when(transactionMonthService.toDto(mockMonth)).thenReturn(expectedDto);

    transactionService.updateTransaction(50L, dto);

    verify(transactionRepository).save(transactionCaptor.capture());
    Transaction captured = transactionCaptor.getValue();
    assertEquals(mockTypeIncome, captured.getTransactionType());
    assertEquals(LocalDate.of(2023, 10, 20), captured.getTransactionDate());
    assertEquals("Updated desc", captured.getDescription());
  }

  @Test
  void testUpdateTransaction_NotFound() {
    when(transactionRepository.findById(50L)).thenReturn(Optional.empty());
    assertThrows(TransactionNotFoundException.class,
        () -> transactionService.updateTransaction(50L, createValidDto(100L)));
  }

  // --- deleteTransaction ---
  @Test
  void testDeleteTransaction_Success() {
    Transaction existingTrans = Transaction.builder().id(50L).transactionMonth(mockMonth).build();
    when(transactionRepository.findById(50L)).thenReturn(Optional.of(existingTrans));
    TransactionMonthDto expectedDto = TransactionMonthDto.builder().build();
    when(transactionMonthService.toDto(mockMonth)).thenReturn(expectedDto);

    transactionService.deleteTransaction(50L);

    verify(transactionRepository).delete(existingTrans);
  }

  @Test
  void testDeleteTransaction_NotFound() {
    when(transactionRepository.findById(50L)).thenReturn(Optional.empty());
    assertThrows(TransactionNotFoundException.class, () -> transactionService.deleteTransaction(50L));
  }

  // --- getLastTransactionValue ---
  @Test
  void testGetLastTransactionValue_WithDescriptionFound() {
    Transaction trans = Transaction.builder().amount(new BigDecimal("99.90")).build();
    when(transactionTypeRepository.findById(100L)).thenReturn(Optional.of(mockTypeIncome));
    when(transactionRepository
        .findFirstByUserAndTransactionTypeAndDescriptionAndAmountGreaterThanOrderByTransactionDateDesc(
            mockUser, mockTypeIncome, "Test desc", BigDecimal.ZERO))
        .thenReturn(Optional.of(trans));

    BigDecimal result = transactionService.getLastTransactionValue(100L, " Test desc ");

    assertEquals(new BigDecimal("99.90"), result);
  }

  @Test
  void testGetLastTransactionValue_WithDescriptionNotFoundFallback() {
    Transaction trans = Transaction.builder().amount(new BigDecimal("50.00")).build();
    when(transactionTypeRepository.findById(100L)).thenReturn(Optional.of(mockTypeIncome));
    when(transactionRepository
        .findFirstByUserAndTransactionTypeAndDescriptionAndAmountGreaterThanOrderByTransactionDateDesc(
            mockUser, mockTypeIncome, "Test desc", BigDecimal.ZERO))
        .thenReturn(Optional.empty());
    when(transactionRepository.findFirstByUserAndTransactionTypeAndAmountGreaterThanOrderByTransactionDateDesc(
        mockUser, mockTypeIncome, BigDecimal.ZERO)).thenReturn(Optional.of(trans));

    BigDecimal result = transactionService.getLastTransactionValue(100L, "Test desc");

    assertEquals(new BigDecimal("50.00"), result);
  }

  @Test
  void testGetLastTransactionValue_NoDescriptionFallback() {
    Transaction trans = Transaction.builder().amount(new BigDecimal("30.00")).build();
    when(transactionTypeRepository.findById(100L)).thenReturn(Optional.of(mockTypeIncome));
    when(transactionRepository.findFirstByUserAndTransactionTypeAndAmountGreaterThanOrderByTransactionDateDesc(
        mockUser, mockTypeIncome, BigDecimal.ZERO)).thenReturn(Optional.of(trans));

    BigDecimal result = transactionService.getLastTransactionValue(100L, null);

    assertEquals(new BigDecimal("30.00"), result);
  }

  @Test
  void testGetLastTransactionValue_NothingFound() {
    when(transactionTypeRepository.findById(100L)).thenReturn(Optional.of(mockTypeIncome));
    when(transactionRepository.findFirstByUserAndTransactionTypeAndAmountGreaterThanOrderByTransactionDateDesc(
        mockUser, mockTypeIncome, BigDecimal.ZERO)).thenReturn(Optional.empty());

    BigDecimal result = transactionService.getLastTransactionValue(100L, null);

    assertEquals(BigDecimal.ZERO, result);
  }

  // --- getSortedTransactions ---
  @Test
  void testGetSortedTransactions() {
    Transaction t1 = Transaction.builder()
        .transactionDate(LocalDate.of(2023, 10, 5))
        .transactionType(mockTypeExpense)
        .amount(new BigDecimal("50.00"))
        .build();

    Transaction t2 = Transaction.builder()
        .transactionDate(LocalDate.of(2023, 10, 5))
        .transactionType(mockTypeIncome)
        .amount(new BigDecimal("100.00"))
        .build();

    Transaction t3 = Transaction.builder()
        .transactionDate(LocalDate.of(2023, 10, 5))
        .transactionType(mockTypeIncome)
        .amount(new BigDecimal("20.00"))
        .build();

    Transaction t4 = Transaction.builder()
        .transactionDate(LocalDate.of(2023, 10, 10))
        .transactionType(mockTypeExpense)
        .amount(new BigDecimal("10.00"))
        .build();

    List<Transaction> list = new ArrayList<>(List.of(t4, t1, t3, t2));
    when(transactionRepository.findByTransactionMonthOrderByTransactionDateAsc(mockMonth)).thenReturn(list);

    List<Transaction> result = transactionService.getSortedTransactions(mockMonth);

    assertEquals(4, result.size());
    // Order should be:
    // 1. t3 (10/5, Income, 20.00)
    // 2. t2 (10/5, Income, 100.00)
    // 3. t1 (10/5, Expense, 50.00)
    // 4. t4 (10/10, Expense, 10.00)
    assertSame(t3, result.get(0));
    assertSame(t2, result.get(1));
    assertSame(t1, result.get(2));
    assertSame(t4, result.get(3));
  }
}
