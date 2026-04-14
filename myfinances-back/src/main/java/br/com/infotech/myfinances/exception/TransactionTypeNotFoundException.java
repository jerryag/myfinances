package br.com.infotech.myfinances.exception;

public class TransactionTypeNotFoundException extends BusinessException {
  public TransactionTypeNotFoundException(String message) {
    super(message);
  }
}
