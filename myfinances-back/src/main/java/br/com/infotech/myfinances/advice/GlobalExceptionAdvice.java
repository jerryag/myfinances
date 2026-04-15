package br.com.infotech.myfinances.advice;

import br.com.infotech.myfinances.exception.BadCredentialsException;
import br.com.infotech.myfinances.exception.BlockedUserException;
import br.com.infotech.myfinances.exception.BusinessException;
import br.com.infotech.myfinances.exception.InfrastructureException;
import br.com.infotech.myfinances.exception.InvalidNewPasswordDataException;
import br.com.infotech.myfinances.exception.ForbiddenSessionException;
import br.com.infotech.myfinances.exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import br.com.infotech.myfinances.domain.MdcKey;
import br.com.infotech.myfinances.util.MdcUtils;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionAdvice extends ResponseEntityExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
    log.debug("Credenciais inválidas: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(ForbiddenSessionException.class)
  public ProblemDetail handleSessionExpired(ForbiddenSessionException ex) {
    log.debug("Sessão inválida ou expirada: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(BlockedUserException.class)
  public ProblemDetail handleBlockedUser(BlockedUserException ex) {
    log.debug("Aviso de usuário bloqueado: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(ValidationException.class)
  public ProblemDetail handleValidation(ValidationException ex) {
    log.debug("Erro de validação: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(InvalidNewPasswordDataException.class)
  public ProblemDetail handleInvalidNewPasswordDataException(InvalidNewPasswordDataException ex) {
    log.debug("Dados de senha inválidos: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    log.debug("Argumento ilegal: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ProblemDetail handleEntityNotFound(EntityNotFoundException ex) {
    log.debug("Entidade não encontrada: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(BusinessException.class)
  public ProblemDetail handleBusiness(BusinessException ex) {
    log.debug("Exceção de negócio: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  @ExceptionHandler(InfrastructureException.class)
  public ProblemDetail handleInfrastructure(InfrastructureException ex) {
    String traceId = MdcUtils.get(MdcKey.TRACE_ID);
    String userId = MdcUtils.get(MdcKey.USER_LOGIN);
    log.error("Exceção de infraestrutura [TraceID: {}, UserID: {}]: ", traceId, userId, ex);
    return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno no sistema.");
  }
}
