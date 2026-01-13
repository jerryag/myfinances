package br.com.infotech.myfinances.repository;

import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.domain.UserStatus;
import br.com.infotech.myfinances.domain.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByLogin_WhenUserIsActive_ShouldReturnUser() {
        User user = User.builder()
                .login("active@email.com")
                .password("pwd")
                .name("Active User")
                .type(UserType.USER)
                .status(UserStatus.ACTIVE)
                .changePwdOnLogin(false)
                .build();

        entityManager.persist(user);
        entityManager.flush();

        Optional<User> found = userRepository.findByLogin("active@email.com");

        assertThat(found).isPresent();
    }

    @Test
    void findByLogin_WhenUserIsDeleted_ShouldReturnEmpty() {
        User user = User.builder()
                .login("deleted@email.com")
                .password("pwd")
                .name("Deleted User")
                .type(UserType.USER)
                // Even if we persist as DELETED, SQLRestriction should hide it
                // BUT: SQLRestriction applies to SELECTs. The persist works.
                .status(UserStatus.DELETED)
                .changePwdOnLogin(false)
                .build();

        entityManager.persist(user);
        entityManager.flush();

        // Clear persistence context to force a Select from DB which triggers
        // SQLRestriction
        entityManager.clear();

        Optional<User> found = userRepository.findByLogin("deleted@email.com");

        assertThat(found).isEmpty();
    }
}
