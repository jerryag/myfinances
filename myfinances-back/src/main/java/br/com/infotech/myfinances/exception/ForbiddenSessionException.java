package br.com.infotech.myfinances.exception;

/**
 * Exceção lançada quando o token de sessão JWT do usuário está ausente,
 * inválido ou com o tempo de expiração esgotado.
 * Mapeada para HTTP 401 Unauthorized.
 */
public class ForbiddenSessionException extends BusinessException {

  public ForbiddenSessionException(String message) {
    super(message);
  }
}
