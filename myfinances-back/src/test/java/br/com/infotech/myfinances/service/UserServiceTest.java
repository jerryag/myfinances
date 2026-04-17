package br.com.infotech.myfinances.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.infotech.myfinances.context.UserContext;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.domain.UserStatus;
import br.com.infotech.myfinances.domain.UserType;
import br.com.infotech.myfinances.dto.UserDto;
import br.com.infotech.myfinances.exception.BadCredentialsException;
import br.com.infotech.myfinances.exception.BlockedUserException;
import br.com.infotech.myfinances.exception.ValidationException;
import br.com.infotech.myfinances.exception.InvalidNewPasswordDataException;
import br.com.infotech.myfinances.exception.LoginAlreadyExistsException;
import br.com.infotech.myfinances.exception.MasterUserNotAllowedException;
import br.com.infotech.myfinances.exception.UserNotAuthenticatedException;
import br.com.infotech.myfinances.exception.UserNotFoundException;
import br.com.infotech.myfinances.repository.UserRepository;
import br.com.infotech.myfinances.util.CryptUtils;

@ExtendWith(SpringExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache cache;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  private final String defaultPassword = "DefaultPassword@123";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(userService, "defaultPassword", defaultPassword);
    UserContext.clear(); // Ensure context is empty before each test

    // Default behavior for cache manager
    when(cacheManager.getCache("users")).thenReturn(cache);
  }

  private User createValidUser(Long id, String login, String password, UserStatus status, UserType type) {
    return User.builder()
        .id(id)
        .login(login)
        .name("Test User")
        .password(CryptUtils.encrypt(password))
        .status(status)
        .type(type)
        .changePwdOnLogin(false)
        .build();
  }

  @Test
  void testFindByLogin_Success() {
    String login = "testuser";
    User mockUser = createValidUser(1L, login, "pass", UserStatus.ACTIVE, UserType.USER);
    when(userRepository.findByLogin(login)).thenReturn(Optional.of(mockUser));

    Optional<User> result = userService.findByLogin(login);
    assertTrue(result.isPresent());
    assertEquals(login, result.get().getLogin());
  }

  @Test
  void testFindByLogin_NullLogin() {
    assertThrows(ValidationException.class, () -> userService.findByLogin(null));
  }

  @Test
  void testLogin_Success() {
    String login = "testuser";
    String plainPassword = "MyPassword@1";
    User mockUser = createValidUser(1L, login, plainPassword, UserStatus.ACTIVE, UserType.USER);

    when(userRepository.findByLogin(login)).thenReturn(Optional.of(mockUser));

    UserDto result = userService.login(login, plainPassword);

    assertNotNull(result);
    assertEquals(login, result.getLogin());
    assertEquals(1L, result.getId());
    assertEquals(mockUser, UserContext.getCurrentUser());
  }

  @Test
  void testLogin_UserNotFound() {
    when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());
    assertThrows(BadCredentialsException.class, () -> userService.login("unknown", "pass"));
  }

  @Test
  void testLogin_WrongPassword() {
    String login = "testuser";
    User mockUser = createValidUser(1L, login, "Correct@123", UserStatus.ACTIVE, UserType.USER);
    when(userRepository.findByLogin(login)).thenReturn(Optional.of(mockUser));

    assertThrows(BadCredentialsException.class, () -> userService.login(login, "WrongPass@1"));
  }

  @Test
  void testLogin_BlockedUser() {
    String login = "blockeduser";
    String plainPassword = "MyPassword@1";
    User mockUser = createValidUser(1L, login, plainPassword, UserStatus.BLOCKED, UserType.USER);
    when(userRepository.findByLogin(login)).thenReturn(Optional.of(mockUser));

    assertThrows(BlockedUserException.class, () -> userService.login(login, plainPassword));
  }

  @Test
  void testChangePassword_Success() {
    Long userId = 1L;
    String oldPassword = "OldPassword@1";
    String newPassword = "NewPassword@2";

    User mockUser = createValidUser(userId, "user1", oldPassword, UserStatus.ACTIVE, UserType.USER);
    mockUser.setChangePwdOnLogin(true);

    when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
    when(userRepository.save(any(User.class))).thenReturn(mockUser);

    userService.changePassword(userId, oldPassword, newPassword);

    verify(userRepository).save(userCaptor.capture());
    verify(cache).evict(mockUser.getLogin());
    User savedUser = userCaptor.getValue();
    assertEquals(CryptUtils.encrypt(newPassword), savedUser.getPassword());
    assertFalse(savedUser.getChangePwdOnLogin());
  }

  @Test
  void testChangePassword_UserNotFound() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(UserNotFoundException.class, () -> userService.changePassword(1L, "old", "NewPass@1"));
  }

  @Test
  void testChangePassword_WrongOldPassword() {
    User mockUser = createValidUser(1L, "user1", "RealOld@1", UserStatus.ACTIVE, UserType.USER);
    when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

    assertThrows(BadCredentialsException.class, () -> userService.changePassword(1L, "WrongOld@1", "NewPass@2"));
  }

  @Test
  void testChangePassword_InvalidNewPassword_Short() {
    User mockUser = createValidUser(1L, "user1", "OldPass@1", UserStatus.ACTIVE, UserType.USER);
    when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

    assertThrows(InvalidNewPasswordDataException.class, () -> userService.changePassword(1L, "OldPass@1", "Sh0r@"));
  }

  @Test
  void testChangePassword_InvalidNewPassword_NoUppercase() {
    User mockUser = createValidUser(1L, "user1", "OldPass@1", UserStatus.ACTIVE, UserType.USER);
    when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

    assertThrows(InvalidNewPasswordDataException.class, () -> userService.changePassword(1L, "OldPass@1", "nouppercase@1"));
  }

  @Test
  void testFindAll() {
    Pageable pageable = PageRequest.of(0, 10);
    User mockUser = createValidUser(1L, "user1", "pass", UserStatus.ACTIVE, UserType.USER);
    Page<User> page = new PageImpl<>(Collections.singletonList(mockUser));

    when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    Page<UserDto> result = userService.findAll(List.of(UserStatus.ACTIVE), pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals("user1", result.getContent().get(0).getLogin());
  }

  @Test
  void testCreate_Success_WithDefaultPassword() {
    UserDto dto = UserDto.builder()
        .login("newuser")
        .name("N User")
        .type(UserType.USER.name())
        .build();

    when(userRepository.findByLogin("newuser")).thenReturn(Optional.empty());
    
    User savedUser = createValidUser(1L, "newuser", defaultPassword, UserStatus.ACTIVE, UserType.USER);
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    UserDto result = userService.create(dto);

    assertNotNull(result);
    assertEquals(1L, result.getId());
    
    verify(userRepository).save(userCaptor.capture());
    User captured = userCaptor.getValue();
    assertEquals(CryptUtils.encrypt(defaultPassword), captured.getPassword());
    assertTrue(captured.getChangePwdOnLogin());
    assertEquals(UserStatus.ACTIVE, captured.getStatus());
  }

  @Test
  void testCreate_LoginAlreadyExists() {
    UserDto dto = UserDto.builder().login("existing").name("Name").type(UserType.USER.name()).build();
    when(userRepository.findByLogin("existing")).thenReturn(Optional.of(new User()));

    assertThrows(LoginAlreadyExistsException.class, () -> userService.create(dto));
  }

  @Test
  void testCreate_MasterTypeNotAllowed() {
    UserDto dto = UserDto.builder().login("newuser").name("Name").type(UserType.MASTER.name()).build();
    when(userRepository.findByLogin("newuser")).thenReturn(Optional.empty());

    assertThrows(MasterUserNotAllowedException.class, () -> userService.create(dto));
  }

  @Test
  void testUpdate_Success() {
    Long id = 1L;
    User mockUser = createValidUser(id, "oldlogin", "pass", UserStatus.ACTIVE, UserType.USER);
    
    UserDto dto = UserDto.builder()
        .login("newlogin")
        .name("New Name")
        .type(UserType.ADMIN.name())
        .password("NewPassForUpdate@1")
        .build();

    when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));
    when(userRepository.findByLogin("newlogin")).thenReturn(Optional.empty());
    
    User updatedMock = createValidUser(id, "newlogin", "NewPassForUpdate@1", UserStatus.ACTIVE, UserType.ADMIN);
    when(userRepository.save(any(User.class))).thenReturn(updatedMock);

    UserDto result = userService.update(id, dto);

    assertNotNull(result);
    assertEquals("newlogin", result.getLogin());
    
    verify(userRepository).save(userCaptor.capture());
    User captured = userCaptor.getValue();
    assertEquals("newlogin", captured.getLogin());
    assertEquals("New Name", captured.getName());
    assertEquals(UserType.ADMIN, captured.getType());
    assertEquals(CryptUtils.encrypt("NewPassForUpdate@1"), captured.getPassword());
    assertTrue(captured.getChangePwdOnLogin());
  }

  @Test
  void testUpdate_LoginAlreadyExists() {
    Long id = 1L;
    User mockUser = createValidUser(id, "oldlogin", "pass", UserStatus.ACTIVE, UserType.USER);
    UserDto dto = UserDto.builder().login("existinglogin").name("Name").type(UserType.USER.name()).build();

    when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));
    when(userRepository.findByLogin("existinglogin")).thenReturn(Optional.of(new User()));

    assertThrows(LoginAlreadyExistsException.class, () -> userService.update(id, dto));
  }

  @Test
  void testChangeStatus_Success() {
    Long id = 1L;
    User mockUser = createValidUser(id, "login", "pass", UserStatus.ACTIVE, UserType.USER);
    when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));
    when(userRepository.save(any(User.class))).thenReturn(mockUser);

    userService.changeStatus(id, UserStatus.BLOCKED);

    verify(userRepository).save(userCaptor.capture());
    verify(cache).evict(mockUser.getLogin());
    assertEquals(UserStatus.BLOCKED, userCaptor.getValue().getStatus());
  }

  @Test
  void testGetCurrentUser_Success() {
    User user = createValidUser(1L, "user1", "pass", UserStatus.ACTIVE, UserType.USER);
    UserContext.setCurrentUser(user);
    
    User result = userService.getCurrentUser();
    assertEquals(user, result);
  }

  @Test
  void testGetCurrentUser_NotAuthenticated() {
    UserContext.clear();
    assertThrows(UserNotAuthenticatedException.class, () -> userService.getCurrentUser());
  }
}
