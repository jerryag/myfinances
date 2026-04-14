package br.com.infotech.myfinances.exception;

public class TransactionNotFoundException extends BusinessException {
  public TransactionNotFoundException(String message) {
    super(message);
  }
}
