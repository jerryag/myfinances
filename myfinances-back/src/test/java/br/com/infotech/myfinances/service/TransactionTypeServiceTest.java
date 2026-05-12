package br.com.infotech.myfinances.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import br.com.infotech.myfinances.domain.TransactionType;
import br.com.infotech.myfinances.domain.TransactionTypeStatus;
import br.com.infotech.myfinances.domain.TransactionTypeType;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.dto.TransactionTypeDto;
import br.com.infotech.myfinances.exception.TransactionTypeChangeTypeException;
import br.com.infotech.myfinances.exception.ValidationException;
import br.com.infotech.myfinances.repository.TransactionTypeRepository;
import br.com.infotech.myfinances.exception.TransactionTypeNotFoundException;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(SpringExtension.class)
class TransactionTypeServiceTest {

  @InjectMocks
  private TransactionTypeService transactionTypeService;

  @Mock
  private TransactionTypeRepository transactionTypeRepository;

  @Mock
  private UserService userService;

  @Captor
  private ArgumentCaptor<TransactionType> transactionTypeCaptor;

  private User mockUser;

  @BeforeEach
  void setUp() {
    mockUser = User.builder().id(1L).login("user1").build();
    when(userService.getCurrentUser()).thenReturn(mockUser);
  }

  // --- Helpers ---
  private TransactionType createValidEntity(Long id, TransactionTypeType type) {
    return TransactionType.builder()
        .id(id)
        .user(mockUser)
        .type(type)
        .description("Test Type")
        .recurring(true)
        .defaultDay(5)
        .defaultAmount(new BigDecimal("100.00"))
        .status(TransactionTypeStatus.ACTIVE)
        .iconName("icon-test")
        .build();
  }

  private TransactionTypeDto createValidDto(TransactionTypeType type) {
    return TransactionTypeDto.builder()
        .type(type)
        .description("Test Type")
        .recurring(true)
        .defaultDay(5)
        .defaultAmount(new BigDecimal("100.00"))
        .iconName("icon-test")
        .build();
  }

  // --- findAll ---
  @Test
  void testFindAll_WithTypes() {
    Pageable pageable = PageRequest.of(0, 10);
    TransactionType mockEntity = createValidEntity(1L, TransactionTypeType.INCOME);
    Page<TransactionType> mockPage = new PageImpl<>(List.of(mockEntity));

    when(transactionTypeRepository.search(
        eq(mockUser), 
        eq(TransactionTypeStatus.ACTIVE), 
        eq("desc"), 
        eq(List.of(TransactionTypeType.INCOME)), 
        eq(pageable)
    )).thenReturn(mockPage);

    Page<TransactionTypeDto> result = transactionTypeService.findAll("desc", List.of("INCOME"), pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals("Test Type", result.getContent().get(0).getDescription());
  }

  @Test
  void testFindAll_NoTypes() {
    Pageable pageable = PageRequest.of(0, 10);
    TransactionType mockEntity = createValidEntity(1L, TransactionTypeType.INCOME);
    Page<TransactionType> mockPage = new PageImpl<>(List.of(mockEntity));

    when(transactionTypeRepository.search(
        eq(mockUser), 
        eq(TransactionTypeStatus.ACTIVE), 
        isNull(), 
        anyList(), 
        eq(pageable)
    )).thenReturn(mockPage);

    Page<TransactionTypeDto> result = transactionTypeService.findAll(null, null, pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
  }

  // --- findById ---
  @Test
  void testFindById_Success() {
    TransactionType mockEntity = createValidEntity(1L, TransactionTypeType.INCOME);
    when(transactionTypeRepository.findById(1L)).thenReturn(Optional.of(mockEntity));

    TransactionTypeDto result = transactionTypeService.findById(1L);

    assertNotNull(result);
    assertEquals(1L, result.getId());
  }

  @Test
  void testFindById_NullId() {
    assertThrows(ValidationException.class, () -> transactionTypeService.findById(null));
  }

  @Test
  void testFindById_NotFoundInDb() {
    when(transactionTypeRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(TransactionTypeNotFoundException.class, () -> transactionTypeService.findById(1L));
  }

  @Test
  void testFindById_WrongUser() {
    TransactionType mockEntity = createValidEntity(1L, TransactionTypeType.INCOME);
    mockEntity.setUser(User.builder().id(999L).build()); // different user
    when(transactionTypeRepository.findById(1L)).thenReturn(Optional.of(mockEntity));

    assertThrows(TransactionTypeNotFoundException.class, () -> transactionTypeService.findById(1L));
  }

  @Test
  void testFindById_StatusNotActive() {
    TransactionType mockEntity = createValidEntity(1L, TransactionTypeType.INCOME);
    mockEntity.setStatus(TransactionTypeStatus.DELETED);
    when(transactionTypeRepository.findById(1L)).thenReturn(Optional.of(mockEntity));

    assertThrows(TransactionTypeNotFoundException.class, () -> transactionTypeService.findById(1L));
  }

  // --- create ---
  @Test
  void testCreate_Success() {
    TransactionTypeDto dto = createValidDto(TransactionTypeType.EXPENSE);
    TransactionType savedEntity = createValidEntity(1L, TransactionTypeType.EXPENSE);
    
    when(transactionTypeRepository.save(any(TransactionType.class))).thenReturn(savedEntity);

    TransactionTypeDto result = transactionTypeService.create(dto);

    assertNotNull(result);
    assertEquals(1L, result.getId());
    
    verify(transactionTypeRepository).save(transactionTypeCaptor.capture());
    TransactionType captured = transactionTypeCaptor.getValue();
    assertEquals("Test Type", captured.getDescription());
    assertEquals(TransactionTypeStatus.ACTIVE, captured.getStatus());
    assertEquals(mockUser, captured.getUser());
    assertTrue(captured.getRecurring());
  }

  @Test
  void testCreate_NullDto() {
    assertThrows(ValidationException.class, () -> transactionTypeService.create(null));
  }
  
  @Test
  void testCreate_MissingType() {
    TransactionTypeDto dto = createValidDto(null);
    assertThrows(ValidationException.class, () -> transactionTypeService.create(dto));
  }

  // --- update ---
  @Test
  void testUpdate_Success() {
    TransactionType mockEntity = createValidEntity(1L, TransactionTypeType.INCOME);
    TransactionTypeDto dto = createValidDto(TransactionTypeType.INCOME);
    dto.setDescription("Updated description");

    when(transactionTypeRepository.findById(1L)).thenReturn(Optional.of(mockEntity));
    when(transactionTypeRepository.save(any(TransactionType.class))).thenReturn(mockEntity);

    TransactionTypeDto result = transactionTypeService.update(1L, dto);

    assertNotNull(result);
    assertEquals("Updated description", result.getDescription());
    
    verify(transactionTypeRepository).save(transactionTypeCaptor.capture());
    assertEquals("Updated description", transactionTypeCaptor.getValue().getDescription());
  }

  @Test
  void testUpdate_ChangeTypeForbidden() {
    TransactionType mockEntity = createValidEntity(1L, TransactionTypeType.INCOME);
    TransactionTypeDto dto = createValidDto(TransactionTypeType.EXPENSE); // trying to change to EXPENSE

    when(transactionTypeRepository.findById(1L)).thenReturn(Optional.of(mockEntity));

    assertThrows(TransactionTypeChangeTypeException.class, () -> transactionTypeService.update(1L, dto));
  }

  // --- delete ---
  @Test
  void testDelete_Success() {
    TransactionType mockEntity = createValidEntity(1L, TransactionTypeType.INCOME);
    when(transactionTypeRepository.findById(1L)).thenReturn(Optional.of(mockEntity));

    transactionTypeService.delete(1L);

    verify(transactionTypeRepository).save(transactionTypeCaptor.capture());
    assertEquals(TransactionTypeStatus.DELETED, transactionTypeCaptor.getValue().getStatus());
  }

  // --- findRecurringActiveByUser ---
  @Test
  void testFindRecurringActiveByUser() {
    TransactionType mockEntity = createValidEntity(1L, TransactionTypeType.EXPENSE);
    when(transactionTypeRepository.findByUserAndStatusAndRecurringTrue(mockUser, TransactionTypeStatus.ACTIVE))
      .thenReturn(List.of(mockEntity));

    List<TransactionType> list = transactionTypeService.findRecurringActiveByUser(mockUser);
    
    assertFalse(list.isEmpty());
    assertEquals(1, list.size());
  }

}
