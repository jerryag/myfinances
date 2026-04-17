package br.com.infotech.myfinances.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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
import org.springframework.test.util.ReflectionTestUtils;

import br.com.infotech.myfinances.domain.Transaction;
import br.com.infotech.myfinances.domain.TransactionMonth;
import br.com.infotech.myfinances.domain.TransactionStatus;
import br.com.infotech.myfinances.domain.TransactionType;
import br.com.infotech.myfinances.domain.TransactionTypeType;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.dto.TransactionDto;
import br.com.infotech.myfinances.dto.TransactionMonthDto;
import br.com.infotech.myfinances.exception.TransactionMonthNotFoundException;
import br.com.infotech.myfinances.exception.ValidationException;
import br.com.infotech.myfinances.repository.TransactionMonthRepository;
import br.com.infotech.myfinances.repository.TransactionRepository;

@ExtendWith(SpringExtension.class)
class TransactionMonthServiceTest {

  @InjectMocks
  private TransactionMonthService transactionMonthService;

  @Mock
  private TransactionMonthRepository transactionMonthRepository;

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private TransactionTypeService transactionTypeService;

  @Mock
  private UserService userService;

  @Mock
  private TransactionService transactionService;

  @Captor
  private ArgumentCaptor<TransactionMonth> monthCaptor;

  @Captor
  private ArgumentCaptor<TransactionDto> dtoCaptor;

  private User mockUser;

  @BeforeEach
  void setUp() {
    mockUser = User.builder().id(1L).login("user1").build();
    when(userService.getCurrentUser()).thenReturn(mockUser);
    ReflectionTestUtils.setField(transactionMonthService, "transactionService", transactionService);
  }

  // --- getOrCreateMonth ---
  @Test
  void testGetOrCreateMonth_AlreadyExists() {
    TransactionMonth existingMonth = TransactionMonth.builder().id(50L).user(mockUser).month(10).year(2023).initialBalance(BigDecimal.ZERO).build();
    when(transactionMonthRepository.findByUserAndMonthAndYear(mockUser, 10, 2023)).thenReturn(Optional.of(existingMonth));
    when(transactionService.getSortedTransactions(existingMonth)).thenReturn(Collections.emptyList());

    TransactionMonthDto result = transactionMonthService.getOrCreateMonth(10, 2023);

    assertNotNull(result);
    assertEquals(50L, result.getId());
    assertEquals(10, result.getMonth());
    assertEquals(2023, result.getYear());
    verify(transactionMonthRepository, times(0)).save(any());
  }

  @Test
  void testGetOrCreateMonth_CreatesNew() {
    when(transactionMonthRepository.findByUserAndMonthAndYear(mockUser, 10, 2023)).thenReturn(Optional.empty());

    TransactionMonth newMonthMock = TransactionMonth.builder().id(99L).user(mockUser).month(10).year(2023).initialBalance(BigDecimal.ZERO).build();
    when(transactionMonthRepository.save(any(TransactionMonth.class))).thenReturn(newMonthMock);

    TransactionType mockType = TransactionType.builder()
        .id(1L)
        .defaultDay(35) // to test resolveDay maxDay logic
        .defaultAmount(new BigDecimal("100.00"))
        .description("Test recurring")
        .build();
        
    when(transactionTypeService.findRecurringActiveByUser(mockUser)).thenReturn(List.of(mockType));
    when(transactionService.getSortedTransactions(newMonthMock)).thenReturn(Collections.emptyList());

    TransactionMonthDto result = transactionMonthService.getOrCreateMonth(10, 2023);

    assertNotNull(result);
    assertEquals(99L, result.getId());
    
    verify(transactionMonthRepository).save(monthCaptor.capture());
    TransactionMonth capturedMonth = monthCaptor.getValue();
    assertEquals(10, capturedMonth.getMonth());
    assertEquals(2023, capturedMonth.getYear());
    assertEquals(mockUser, capturedMonth.getUser());
    assertEquals(BigDecimal.ZERO, capturedMonth.getInitialBalance());

    verify(transactionService).addTransaction(eq(99L), dtoCaptor.capture());
    TransactionDto capturedDto = dtoCaptor.getValue();
    assertEquals(31, capturedDto.getDay()); // October has 31 days
    assertEquals(new BigDecimal("100.00"), capturedDto.getAmount());
    assertEquals("Test recurring", capturedDto.getDescription());
    assertEquals(TransactionStatus.PENDING, capturedDto.getStatus());
  }

  // --- updateInitialBalance ---
  @Test
  void testUpdateInitialBalance_Success() {
    TransactionMonth existingMonth = TransactionMonth.builder().id(50L).user(mockUser).month(10).year(2023).initialBalance(BigDecimal.ZERO).build();
    when(transactionMonthRepository.findById(50L)).thenReturn(Optional.of(existingMonth));
    when(transactionMonthRepository.save(any(TransactionMonth.class))).thenReturn(existingMonth);
    when(transactionService.getSortedTransactions(existingMonth)).thenReturn(Collections.emptyList());

    TransactionMonthDto result = transactionMonthService.updateInitialBalance(50L, new BigDecimal("150.00"));

    assertNotNull(result);
    verify(transactionMonthRepository).save(monthCaptor.capture());
    assertEquals(new BigDecimal("150.00"), monthCaptor.getValue().getInitialBalance());
  }

  @Test
  void testUpdateInitialBalance_NullId() {
    assertThrows(ValidationException.class, () -> transactionMonthService.updateInitialBalance(null, BigDecimal.ZERO));
  }

  @Test
  void testUpdateInitialBalance_NullBalance() {
    assertThrows(ValidationException.class, () -> transactionMonthService.updateInitialBalance(50L, null));
  }

  @Test
  void testUpdateInitialBalance_NotFound() {
    when(transactionMonthRepository.findById(50L)).thenReturn(Optional.empty());
    assertThrows(TransactionMonthNotFoundException.class, () -> transactionMonthService.updateInitialBalance(50L, BigDecimal.ZERO));
  }

  // --- toDto (Mapping Logic) ---
  @Test
  void testToDtoMapping() {
    TransactionMonth existingMonth = TransactionMonth.builder().id(50L).user(mockUser).month(10).year(2023).status("OPEN").initialBalance(new BigDecimal("200.00")).build();
    
    TransactionType mockType = TransactionType.builder().id(1L).type(TransactionTypeType.EXPENSE).build();

    Transaction t1 = Transaction.builder()
        .id(100L)
        .transactionDate(LocalDate.of(2023, 10, 5))
        .transactionType(mockType)
        .description("Tr 1")
        .amount(new BigDecimal("50.00"))
        .status(TransactionStatus.COMPLETED)
        .remark("Test remark")
        .iconName("test-icon")
        .build();

    TransactionDto t1Dto = TransactionDto.builder()
        .id(100L)
        .day(5)
        .transactionTypeId(1L)
        .description("Tr 1")
        .amount(new BigDecimal("50.00"))
        .status(TransactionStatus.COMPLETED)
        .remark("Test remark")
        .iconName("test-icon")
        .build();

    when(transactionService.getSortedTransactions(existingMonth)).thenReturn(List.of(t1Dto));

    TransactionMonthDto result = transactionMonthService.toDto(existingMonth);

    assertEquals(50L, result.getId());
    assertEquals(10, result.getMonth());
    assertEquals(2023, result.getYear());
    assertEquals("OPEN", result.getStatus());
    assertEquals(new BigDecimal("200.00"), result.getInitialBalance());
    assertEquals(1, result.getTransactions().size());
    
    TransactionDto tDto = result.getTransactions().get(0);
    assertEquals(100L, tDto.getId());
    assertEquals(5, tDto.getDay());
    assertEquals(1L, tDto.getTransactionTypeId());
    assertEquals("Tr 1", tDto.getDescription());
    assertEquals(new BigDecimal("50.00"), tDto.getAmount());
    assertEquals(TransactionStatus.COMPLETED, tDto.getStatus());
    assertEquals("Test remark", tDto.getRemark());
    assertEquals("test-icon", tDto.getIconName());
  }

}
