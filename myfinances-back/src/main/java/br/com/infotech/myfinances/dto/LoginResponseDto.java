package br.com.infotech.myfinances.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO de resposta ao login do usuário.
 * Contém os dados do usuário autenticado e o token JWT de sessão.
 */
@Getter
@Builder
public class LoginResponseDto {

  /** Token JWT de sessão. Deve ser enviado no header {@code X-Session-Token} nas próximas requisições. */
  private String token;

  private Long id;
  private String login;
  private String name;
  private String type;
  private Boolean changePwdOnLogin;
}
