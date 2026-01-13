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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final String INVALID_CREDENTIALS_MSG = "Usuário ou senha inválidos";

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Busca um usuário pelo login informado.
     *
     * @param login O login do usuário.
     * @return Um Optional contendo o usuário, se encontrado.
     * @throws IllegalArgumentException se o login for nulo ou vazio.
     */
    public Optional<User> findByLogin(String login) {
        if (StringUtils.isBlank(login)) {
            throw new IllegalArgumentException("Login obrigatório");
        }
        return userRepository.findByLogin(login);
    }

    /**
     * Realiza o login do usuário verificando as credenciais e o status.
     *
     * @param login    O login do usuário.
     * @param password A senha do usuário.
     * @return Um DTO com as informações do usuário autenticado.
     * @throws IllegalArgumentException se o login ou senha forem nulos ou vazios.
     * @throws BadCredentialsException  se o usuário não for encontrado, a senha
     *                                  estiver incorreta.
     * @throws BlockedUserException     se o usuário estiver com status BLOCKED.
     */
    public UserDto login(String login, String password) {
        if (StringUtils.isBlank(login) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("Login e senha obrigatórios");
        }

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

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (StringUtils.isBlank(oldPassword) || StringUtils.isBlank(newPassword)) {
            throw new IllegalArgumentException("Senha atual e nova senha são obrigatórias");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

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
        // Minimum 8 characters
        if (password.length() < 8) {
            throw new InvalidNewPasswordDataException(
                    "A senha deve ter no mínimo 8 caracteres");
        }
        // At least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            throw new InvalidNewPasswordDataException(
                    "A senha deve conter ao menos uma letra maiúscula");
        }
        // At least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            throw new InvalidNewPasswordDataException(
                    "A senha deve conter ao menos uma letra minúscula");
        }
        // At least one number
        if (!password.matches(".*[0-9].*")) {
            throw new InvalidNewPasswordDataException(
                    "A senha deve conter ao menos um número");
        }
        // At least one symbol (non-alphanumeric)
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            throw new InvalidNewPasswordDataException(
                    "A senha deve conter ao menos um símbolo");
        }
    }

    public Page<UserDto> findAll(List<UserStatus> statuses,
            Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            if (statuses != null && !statuses.isEmpty()) {
                return cb.and(root.get("status").in(statuses),
                        cb.notEqual(root.get("type"), UserType.MASTER));
            }
            return cb.notEqual(root.get("type"), UserType.MASTER);
        };

        return userRepository.findAll(spec, pageable).map(user -> UserDto.builder()
                .id(user.getId())
                .login(user.getLogin())
                .name(user.getName())
                .type(user.getType().name())
                .changePwdOnLogin(user.getChangePwdOnLogin())
                .status(user.getStatus().name())
                .build());
    }

    @Transactional
    public UserDto create(UserDto userDto) {
        if (StringUtils.isBlank(userDto.getLogin()) || StringUtils.isBlank(userDto.getName())
                || StringUtils.isBlank(userDto.getType())) {
            throw new IllegalArgumentException("Dados obrigatórios não preenchidos");
        }

        userRepository.findByLogin(userDto.getLogin()).ifPresent(u -> {
            throw new IllegalArgumentException("Login já existe");
        });

        if (UserType.MASTER.name().equals(userDto.getType())) {
            throw new IllegalArgumentException("Não é permitido criar usuário MASTER");
        }

        String password = StringUtils.isNotBlank(userDto.getPassword())
                ? userDto.getPassword()
                : "MyFinances@123";

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

    @Transactional
    public UserDto update(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!user.getLogin().equals(userDto.getLogin())) {
            userRepository.findByLogin(userDto.getLogin()).ifPresent(u -> {
                throw new IllegalArgumentException("Login já existe");
            });
            user.setLogin(userDto.getLogin());
        }

        if (UserType.MASTER.name().equals(userDto.getType())) {
            throw new IllegalArgumentException("Não é permitido alterar para tipo MASTER");
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

    @Transactional
    public void changeStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        user.setStatus(status);
        userRepository.save(user);
    }
}
