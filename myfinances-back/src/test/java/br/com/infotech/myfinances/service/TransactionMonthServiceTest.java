package br.com.infotech.myfinances.service;

import br.com.infotech.myfinances.domain.*;
import br.com.infotech.myfinances.dto.TransactionMonthDto;
import br.com.infotech.myfinances.repository.TransactionMonthRepository;
import br.com.infotech.myfinances.repository.TransactionRepository;
import br.com.infotech.myfinances.repository.TransactionTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class TransactionMonthServiceTest {

  @Mock
  private TransactionMonthRepository transactionMonthRepository;

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private TransactionTypeRepository transactionTypeRepository;

  @Mock
  private UserService userService;

  @InjectMocks
  private TransactionMonthService transactionMonthService;

  @Test
  void getOrCreateMonth_WhenMonthDoesNotExist_ShouldCreateMonthAndRecurringTransactionsWithDefaultValues() {
    // Arrange
    User user = new User();
    user.setId(1L);

    when(userService.getCurrentUser()).thenReturn(user);
    when(transactionMonthRepository.findByUserAndMonthAndYear(user, 5, 2026)).thenReturn(Optional.empty());

    TransactionMonth newMonth = new TransactionMonth();
    newMonth.setId(10L);
    newMonth.setUser(user);
    when(transactionMonthRepository.save(any(TransactionMonth.class))).thenReturn(newMonth);

    TransactionType recurringType = new TransactionType();
    recurringType.setId(100L);
    recurringType.setUser(user);
    recurringType.setType(TransactionTypeType.EXPENSE);
    recurringType.setRecurring(true);
    recurringType.setStatus(TransactionTypeStatus.ACTIVE);
    recurringType.setDefaultDay(15);
    recurringType.setDefaultAmount(new BigDecimal("250.00"));
    recurringType.setDescription("Internet Fixa");

    when(transactionTypeRepository.findAll()).thenReturn(List.of(recurringType));

    Transaction mockedSavedTransaction = new Transaction();
    mockedSavedTransaction.setId(1000L);
    mockedSavedTransaction.setTransactionType(recurringType);
    mockedSavedTransaction.setTransactionDate(java.time.LocalDate.of(2026, 5, 15));
    mockedSavedTransaction.setAmount(new BigDecimal("250.00"));
    mockedSavedTransaction.setDescription("Internet Fixa");
    when(transactionRepository.save(any(Transaction.class))).thenReturn(mockedSavedTransaction);

    when(transactionRepository.findByTransactionMonthOrderByTransactionDateAsc(newMonth))
        .thenReturn(List.of(mockedSavedTransaction));

    // Act
    TransactionMonthDto result = transactionMonthService.getOrCreateMonth(5, 2026);

    // Assert
    assertNotNull(result);

    ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
    verify(transactionRepository, times(1)).save(txCaptor.capture());

    Transaction capturedTransaction = txCaptor.getValue();
    assertEquals(new BigDecimal("250.00"), capturedTransaction.getAmount());
    assertEquals("Internet Fixa", capturedTransaction.getDescription());
    assertEquals(15, capturedTransaction.getTransactionDate().getDayOfMonth());
    assertEquals(5, capturedTransaction.getTransactionDate().getMonthValue());
    assertEquals(2026, capturedTransaction.getTransactionDate().getYear());
  }
}
