package br.com.infotech.myfinances.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa um usuário do sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"user\"")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O login do usuário (único).
     */
    @NotBlank
    @Column(nullable = false, length = 255)
    private String login;

    /**
     * O nome completo do usuário.
     */
    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * A senha do usuário.
     */
    @NotBlank
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * O status do usuário.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /**
     * O tipo do usuário.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType type;

    /**
     * Define se o usuário deve alterar a senha no login.
     */
    @NotNull
    @Column(name = "change_pwd_on_login", nullable = false)
    @Builder.Default
    private Boolean changePwdOnLogin = true;
}
