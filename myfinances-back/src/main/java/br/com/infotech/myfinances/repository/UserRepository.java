package br.com.infotech.myfinances.repository;

import br.com.infotech.myfinances.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Recupera um usuário pelo login, ignorando usuários com status DELETED.
     */
    @Query("SELECT u FROM User u WHERE u.login = :login AND u.status <> br.com.infotech.myfinances.domain.UserStatus.DELETED")
    Optional<User> findByLogin(@Param("login") String login);
}
