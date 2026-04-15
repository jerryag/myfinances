package br.com.infotech.myfinances.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriedades de configuração do JWT de sessão de usuário.
 * Configuradas via prefixo {@code app.jwt} no application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class AppJwtProperties {

  /** Chave secreta HMAC (HS256) para assinar os tokens de sessão. */
  private String secret;

  /**
   * Tempo de expiração da sessão por inatividade (em minutos).
   * O token é renovado a cada requisição bem-sucedida.
   */
  private int expirationMinutes = 10;
}
