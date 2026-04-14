package br.com.infotech.myfinances.util;

import br.com.infotech.myfinances.domain.MdcKey;
import org.slf4j.MDC;
import java.util.UUID;

public class MdcUtils {

  private MdcUtils() {
    // Esconder construtor utilitário
  }

  /**
   * Define um valor no contexto MDC do Logback para a chave informada.
   * <p>
   * O valor só é inserido se não for nulo. A chave é obtida via
   * {@link MdcKey#getKey()}.
   * </p>
   *
   * @param key   a chave MDC, representada por um valor de {@link MdcKey}.
   * @param value o valor a ser associado à chave; ignorado se nulo.
   */
  public static void set(MdcKey key, String value) {
    if (value != null) {
      MDC.put(key.getKey(), value);
    }
  }

  /**
   * Recupera um valor do contexto MDC para a chave informada.
   * <p>
   * O valor é obtido via {@link MdcKey#getKey()}.
   * </p>
   *
   * @param key a chave MDC, representada por um valor de {@link MdcKey}.
   * @return o valor associado à chave no MDC, ou {@code null} se não estiver
   *         definido.
   */
  public static String get(MdcKey key) {
    return MDC.get(key.getKey());
  }

  /**
   * Limpa todo o contexto MDC do Logback para a thread atual.
   * <p>
   * Deve ser invocado ao final do processamento da requisição para evitar
   * vazamento
   * de dados entre requisições distintas na mesma thread.
   * </p>
   */
  public static void clear() {
    MDC.clear();
  }

  /**
   * Gera um TraceId único, registra no contexto MDC e o retorna.
   * <p>
   * O TraceId é gerado a partir de um {@link UUID} sem hifens, utilizando os 10
   * primeiros caracteres
   * do resultado. Em seguida, é automaticamente inserido no MDC via
   * {@link #setMdc(MdcKey, String)}
   * sob a chave {@link MdcKey#TRACE_ID}, permitindo rastrear eventos de log por
   * requisição.
   * </p>
   *
   * @return o TraceId gerado e já registrado no MDC.
   */
  public static String defineTraceId() {
    String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    set(MdcKey.TRACE_ID, traceId);
    return traceId;
  }
}
