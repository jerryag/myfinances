package br.com.infotech.myfinances.service;

import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void findByLogin_WhenLoginIsValid_ShouldReturnUser() {
        String login = "admin@infotech.com";
        User user = new User();
        user.setLogin(login);

        when(userRepository.findByLogin(login)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByLogin(login);

        assertTrue(result.isPresent());
        assertEquals(login, result.get().getLogin());
    }

    @Test
    void findByLogin_WhenLoginIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> userService.findByLogin(null));
    }

    @Test
    void findByLogin_WhenLoginIsEmpty_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> userService.findByLogin(""));
        assertThrows(IllegalArgumentException.class, () -> userService.findByLogin("   "));
    }
}
