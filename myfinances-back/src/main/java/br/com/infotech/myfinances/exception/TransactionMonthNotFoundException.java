package br.com.infotech.myfinances.exception;

public class TransactionMonthNotFoundException extends BusinessException {
  public TransactionMonthNotFoundException(String message) {
    super(message);
  }
}
