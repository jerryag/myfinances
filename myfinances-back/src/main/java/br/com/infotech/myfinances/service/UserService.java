package br.com.infotech.myfinances.service;

import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.domain.UserStatus;
import br.com.infotech.myfinances.domain.UserType;
import br.com.infotech.myfinances.dto.UserDto;
import br.com.infotech.myfinances.exception.BadCredentialsException;
import br.com.infotech.myfinances.exception.BlockedUserException;
import br.com.infotech.myfinances.exception.InvalidNewPasswordDataException;
import br.com.infotech.myfinances.repository.UserRepository;
import br.com.infotech.myfinances.util.CryptUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import br.com.infotech.myfinances.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Propagation;
import br.com.infotech.myfinances.exception.UserNotFoundException;
import br.com.infotech.myfinances.exception.LoginAlreadyExistsException;
import br.com.infotech.myfinances.exception.MasterUserNotAllowedException;
import br.com.infotech.myfinances.exception.UserNotAuthenticatedException;
import br.com.infotech.myfinances.exception.BusinessException;
import br.com.infotech.myfinances.util.ValidationUtils;
import br.com.infotech.myfinances.context.UserContext;

@Service
@Transactional(propagation = Propagation.SUPPORTS)
@Slf4j
public class UserService {

  private static final String INVALID_CREDENTIALS_MSG = "Usuário ou senha inválidos";

  private final UserRepository userRepository;

  @Value("${app.params.defaultPassword}")
  private String defaultPassword;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Busca um usuário pelo login informado.
   *
   * @param login O login do usuário.
   * @return Um Optional contendo o usuário, se encontrado.
   * @throws ValidationException se o login for nulo ou vazio.
   */
  public Optional<User> findByLogin(String login) {
    log.debug("Buscando usuário pelo login: {}", login);
    ValidationUtils.hasText(login, "Login obrigatório");
    return userRepository.findByLogin(login);
  }

  /**
   * Realiza o login do usuário verificando as credenciais e o status.
   *
   * @param login    O login do usuário.
   * @param password A senha do usuário.
   * @return Um DTO com as informações do usuário autenticado.
   * @throws ValidationException se o login ou senha forem nulos ou vazios.
   * @throws BadCredentialsException se o usuário não for encontrado, ou a senha estiver incorreta.
   * @throws BlockedUserException se o usuário estiver com status BLOCKED.
   */
  public UserDto login(String login, String password) {
    log.debug("Iniciando rotina de login para o usuário: {}", login);
    ValidationUtils.hasText(login, "Login obrigatório");
    ValidationUtils.hasText(password, "Senha obrigatória");

    User user = userRepository.findByLogin(login)
        .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MSG));

    String encryptedPassword = CryptUtils.encrypt(password);
    if (!user.getPassword().equals(encryptedPassword)) {
      throw new BadCredentialsException(INVALID_CREDENTIALS_MSG);
    }

    if (UserStatus.BLOCKED.equals(user.getStatus())) {
      throw new BlockedUserException("Usuário bloqueado");
    }

    return UserDto.builder()
        .id(user.getId())
        .login(user.getLogin())
        .name(user.getName())
        .type(user.getType().name())
        .changePwdOnLogin(user.getChangePwdOnLogin())
        .build();
  }

  /**
   * Altera a senha do usuário.
   *
   * @param userId      O ID do usuário.
   * @param oldPassword A senha atual.
   * @param newPassword A nova senha desejada.
   * @throws ValidationException se as senhas não forem fornecidas.
   * @throws BusinessException se o usuário não for encontrado.
   * @throws BadCredentialsException se a senha atual informada estiver incorreta.
   * @throws InvalidNewPasswordDataException se a nova senha não obedecer aos critérios de segurança.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void changePassword(Long userId, String oldPassword, String newPassword) {
    log.debug("Iniciando troca de senha para o usuário ID: {}", userId);
    ValidationUtils.hasText(oldPassword, "Senha atual é obrigatória");
    ValidationUtils.hasText(newPassword, "Nova senha é obrigatória");

    User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

    String encryptedOldPassword = CryptUtils.encrypt(oldPassword);
    if (!user.getPassword().equals(encryptedOldPassword)) {
      throw new BadCredentialsException("Senha atual incorreta");
    }

    validateNewPassword(newPassword);

    user.setPassword(CryptUtils.encrypt(newPassword));
    user.setChangePwdOnLogin(false);
    userRepository.save(user);
  }

  private void validateNewPassword(String password) {
    // Mínimo de 8 caracteres
    if (password.length() < 8) {
      throw new InvalidNewPasswordDataException("A senha deve ter no mínimo 8 caracteres");
    }
    // Pelo menos uma letra maiúscula
    if (!password.matches(".*[A-Z].*")) {
      throw new InvalidNewPasswordDataException("A senha deve conter ao menos uma letra maiúscula");
    }
    // Pelo menos uma letra minúscula
    if (!password.matches(".*[a-z].*")) {
      throw new InvalidNewPasswordDataException("A senha deve conter ao menos uma letra minúscula");
    }
    // Pelo menos um número
    if (!password.matches(".*[0-9].*")) {
      throw new InvalidNewPasswordDataException("A senha deve conter ao menos um número");
    }
    // Pelo menos um caractere especial (símbolo não alfanumérico)
    if (!password.matches(".*[^a-zA-Z0-9].*")) {
      throw new InvalidNewPasswordDataException("A senha deve conter ao menos um símbolo");
    }
  }

  /**
   * Busca todos os usuários com suporte a paginação e filtro de status.
   *
   * @param statuses Lista de status para filtrar.
   * @param pageable Configurações de paginação.
   * @return Uma página de usuários em formato DTO.
   */
  public Page<UserDto> findAll(List<UserStatus> statuses, Pageable pageable) {
    log.debug("Buscando todos os usuários filtrados pelos status: {}", statuses);
    Specification<User> spec = (root, query, cb) -> {
      if (statuses != null && !statuses.isEmpty()) {
        return cb.and(root.get("status").in(statuses), cb.notEqual(root.get("type"), UserType.MASTER));
      }
      return cb.notEqual(root.get("type"), UserType.MASTER);
    };

    return userRepository.findAll(spec, pageable)
        .map(user -> UserDto.builder()
            .id(user.getId())
            .login(user.getLogin())
            .name(user.getName())
            .type(user.getType().name())
            .changePwdOnLogin(user.getChangePwdOnLogin())
            .status(user.getStatus().name())
            .build());
  }

  /**
   * Cria um novo usuário no sistema.
   *
   * @param userDto DTO contendo os dados do usuário a ser criado.
   * @return O DTO do usuário criado.
   * @throws ValidationException se algum campo obrigatório não estiver preenchido.
   * @throws BusinessException se o login já existir ou se o tipo de usuário for MASTER.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public UserDto create(UserDto userDto) {
    log.debug("Iniciando criação de novo usuário: {}", userDto != null ? userDto.getLogin() : null);
    ValidationUtils.notNull(userDto, "Objeto usuário obrigatório");
    ValidationUtils.hasText(userDto.getLogin(), "Login obrigatório");
    ValidationUtils.hasText(userDto.getName(), "Nome obrigatório");
    ValidationUtils.hasText(userDto.getType(), "Tipo obrigatório");

    userRepository.findByLogin(userDto.getLogin()).ifPresent(u -> {
      throw new LoginAlreadyExistsException("Login já existe");
    });

    if (UserType.MASTER.name().equals(userDto.getType())) {
      throw new MasterUserNotAllowedException("Não é permitido criar usuário MASTER");
    }

    String password = StringUtils.isNotBlank(userDto.getPassword()) ? userDto.getPassword() : defaultPassword;

    User user = User.builder()
        .login(userDto.getLogin())
        .name(userDto.getName())
        .password(CryptUtils.encrypt(password))
        .status(UserStatus.ACTIVE)
        .type(UserType.valueOf(userDto.getType()))
        .changePwdOnLogin(true)
        .build();

    user = userRepository.save(user);

    return UserDto.builder()
        .id(user.getId())
        .login(user.getLogin())
        .name(user.getName())
        .type(user.getType().name())
        .changePwdOnLogin(user.getChangePwdOnLogin())
        .status(user.getStatus().name())
        .build();
  }

  /**
   * Atualiza os dados de um usuário existente.
   *
   * @param id      ID do usuário.
   * @param userDto Novos dados do usuário.
   * @return O DTO do usuário atualizado.
   * @throws ValidationException se os dados forem inválidos.
   * @throws BusinessException se o login já existir, se o usuário não for encontrado ou se houver tentativa de mudar para MASTER.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public UserDto update(Long id, UserDto userDto) {
    log.debug("Iniciando atualização do usuário com ID: {}", id);
    ValidationUtils.notNull(userDto, "Objeto usuário obrigatório");
    ValidationUtils.hasText(userDto.getLogin(), "Login obrigatório");
    ValidationUtils.hasText(userDto.getName(), "Nome obrigatório");
    ValidationUtils.hasText(userDto.getType(), "Tipo obrigatório");

    User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

    if (!user.getLogin().equals(userDto.getLogin())) {
      userRepository.findByLogin(userDto.getLogin()).ifPresent(u -> {
        throw new LoginAlreadyExistsException("Login já existe");
      });
      user.setLogin(userDto.getLogin());
    }

    if (UserType.MASTER.name().equals(userDto.getType())) {
      throw new MasterUserNotAllowedException("Não é permitido alterar para tipo MASTER");
    }

    user.setName(userDto.getName());
    user.setType(UserType.valueOf(userDto.getType()));

    if (StringUtils.isNotBlank(userDto.getPassword())) {
      user.setPassword(CryptUtils.encrypt(userDto.getPassword()));
      user.setChangePwdOnLogin(true);
    }

    user = userRepository.save(user);

    return UserDto.builder()
        .id(user.getId())
        .login(user.getLogin())
        .name(user.getName())
        .type(user.getType().name())
        .changePwdOnLogin(user.getChangePwdOnLogin())
        .status(user.getStatus().name())
        .build();
  }

  /**
   * Altera o status de um usuário.
   *
   * @param id     ID do usuário.
   * @param status Novo status.
   * @throws BusinessException se o usuário não for encontrado.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void changeStatus(Long id, UserStatus status) {
    log.debug("Iniciando alteração de status para o usuário ID: {} para o status: {}", id, status);
    ValidationUtils.notNull(status, "Status obrigatório");
    User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

    user.setStatus(status);
    userRepository.save(user);
  }

  /**
   * Recupera o usuário no contexto de segurança atual.
   *
   * @return O usuário do contexto.
   * @throws BusinessException se não houver usuário autenticado no contexto.
   */
  public User getCurrentUser() {
    User user = UserContext.getCurrentUser();
    if (user == null) {
      throw new UserNotAuthenticatedException("Usuário não autenticado no contexto.");
    }
    return user;
  }
}
