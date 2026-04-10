package br.com.infotech.myfinances.advice;

import br.com.infotech.myfinances.exception.BadCredentialsException;
import br.com.infotech.myfinances.exception.BlockedUserException;
import br.com.infotech.myfinances.exception.BusinessException;
import br.com.infotech.myfinances.exception.InfrastructureException;
import br.com.infotech.myfinances.exception.InvalidNewPasswordDataException;
import br.com.infotech.myfinances.exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionAdvice extends ResponseEntityExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
    log.warn("Bad credentials: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(BlockedUserException.class)
  public ProblemDetail handleBlockedUser(BlockedUserException ex) {
    log.warn("Blocked user warning: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(ValidationException.class)
  public ProblemDetail handleValidation(ValidationException ex) {
    log.warn("Validation error: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(InvalidNewPasswordDataException.class)
  public ProblemDetail handleInvalidNewPasswordDataException(InvalidNewPasswordDataException ex) {
    log.warn("Invalid password data: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Illegal argument: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ProblemDetail handleEntityNotFound(EntityNotFoundException ex) {
    log.warn("Entity not found: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(BusinessException.class)
  public ProblemDetail handleBusiness(BusinessException ex) {
    log.warn("Business exception: {}", ex.getMessage());
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  @ExceptionHandler(InfrastructureException.class)
  public ProblemDetail handleInfrastructure(InfrastructureException ex) {
    log.error("Infrastructure exception: ", ex);
    return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An internal system error occurred.");
  }
}
