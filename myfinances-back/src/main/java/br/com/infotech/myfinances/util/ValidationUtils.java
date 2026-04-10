package br.com.infotech.myfinances.util;

import br.com.infotech.myfinances.exception.ValidationException;
import org.springframework.util.Assert;

/**
 * Utilitário de validação de argumentos inspirado na classe {@link org.springframework.util.Assert}.
 * Sempre dispara {@link ValidationException} em caso de inconsistência, permitindo o engatilho automático pelo {@link br.com.infotech.myfinances.advice.GlobalExceptionAdvice}.
 */
public class ValidationUtils {

  private ValidationUtils() {}

  /**
   * Valida que o objeto informado não seja nulo.
   *
   * @param object  O objeto a ser inspecionado.
   * @param message A mensagem a ser apresentada na exceção caso a condição falhe.
   * @throws ValidationException se o objeto avaliado for {@code null}.
   */
  public static void notNull(Object object, String message) {
    try {
      Assert.notNull(object, message);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(message, e);
    }
  }

  /**
   * Valida que o texto informado possua algum conteúdo (não nulo, não vazio e com caracteres visíveis).
   *
   * @param text    A {@link String} a ser inspecionada.
   * @param message A mensagem a ser apresentada na exceção caso a condição falhe.
   * @throws ValidationException se a string avaliada carecer de texto.
   */
  public static void hasText(String text, String message) {
    try {
      Assert.hasText(text, message);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(message, e);
    }
  }

  /**
   * Valida que uma expressão de negócio informada seja avaliada como verdadeira.
   *
   * @param expression A expressão booleana a ser validada.
   * @param message    A mensagem de erro lançada.
   * @throws ValidationException caso a expressão seja avaliada como falsa.
   */
  public static void isTrue(boolean expression, String message) {
    try {
      Assert.isTrue(expression, message);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(message, e);
    }
  }

  /**
   * Valida que uma coleção ({@link java.util.Collection}) informada não seja vazia.
   *
   * @param collection A coleção a ser validada.
   * @param message    A mensagem de erro lançada.
   * @throws ValidationException se a dada coleção for nula ou não detiver elementos.
   */
  public static void notEmpty(java.util.Collection<?> collection, String message) {
    try {
      Assert.notEmpty(collection, message);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(message, e);
    }
  }
}
