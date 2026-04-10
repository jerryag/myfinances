package br.com.infotech.myfinances.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitário para criptografia (hashing) de strings.
 */
public class CryptUtils {

  private CryptUtils() {
    // Construtor privado para impedir instânciação
  }

  /**
   * Criptografa uma {@link String} usando o algoritmo SHA-256.
   *
   * @param value A string a ser criptografada.
   * @return A string criptografada em formato hexadecimal.
   * @throws br.com.infotech.myfinances.exception.InfrastructureException caso o algoritmo SHA-256 não seja encontrado em {@link MessageDigest}.
   */
  public static String encrypt(String value) {
    if (value == null) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedhash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(encodedhash);
    } catch (NoSuchAlgorithmException e) {
      throw new br.com.infotech.myfinances.exception.InfrastructureException("Erro ao criptografar string: algoritmo SHA-256 não encontrado", e);
    }
  }

  private static String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
