package br.com.infotech.myfinances.service;

import java.time.Instant;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import br.com.infotech.myfinances.config.AppJwtProperties;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.exception.InfrastructureException;
import br.com.infotech.myfinances.exception.ForbiddenSessionException;
import br.com.infotech.myfinances.exception.UserNotFoundException;
import br.com.infotech.myfinances.util.CryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço responsável por gerar e validar tokens JWT de sessão de usuário.
 *
 * <p>
 * Este JWT é distinto do token M2M do Keycloak. Sua finalidade é autenticar
 * e autorizar o usuário humano que opera o frontend, com expiração deslizante
 * (sliding expiration): o token é renovado a cada requisição bem-sucedida.
 * </p>
 *
 * <p>
 * Claims do token:
 * </p>
 * <ul>
 * <li>{@code sub} — login do usuário</li>
 * <li>{@code exp} — instante de expiração</li>
 * <li>{@code iat} — instante de emissão</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtSessionService {

  private final AppJwtProperties jwtProperties;

  private final UserService userService;

  /**
   * Resultado da validação de um token JWT de sessão.
   *
   * @param login          login do usuário extraído da claim {@code sub}
   * @param refreshedToken novo token com tempo de expiração renovado
   */
  public record SessionClaims(String login, String refreshedToken) {
  }

  /**
   * Gera um novo token JWT de sessão para o usuário informado.
   *
   * @param login login do usuário autenticado
   * @param jwtId jwtId do token JWT
   * @return token JWT assinado com HS256
   */
  public String generateSessionToken(String login, String jwtId) {
    try {

      Instant now = Instant.now();
      Instant expiry = now.plusSeconds(jwtProperties.getExpirationMinutes() * 60L);

      JWTClaimsSet claims = new JWTClaimsSet.Builder()
          .subject(login)
          .jwtID(jwtId)
          .issueTime(Date.from(now))
          .expirationTime(Date.from(expiry))
          .build();

      JWSSigner signer = new MACSigner(jwtProperties.getSecret().getBytes());
      SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      signedJWT.sign(signer);

      log.debug("Token de sessão gerado para o usuário '{}', expira em {} minutos", login,
          jwtProperties.getExpirationMinutes());
      return signedJWT.serialize();

    } catch (Exception e) {
      throw new InfrastructureException("Erro ao gerar token de sessão", e);
    }
  }

  /**
   * Valida um token JWT de sessão e, se válido, renova seu tempo de expiração.
   *
   * @param token token JWT recebido no header {@code X-Session-Token}
   * @return {@link SessionClaims} contendo o login do usuário e o token renovado
   * @throws ForbiddenSessionException se o token estiver expirado,
   *                                   inválido ou com
   *                                   assinatura incorreta
   */
  public SessionClaims validateAndRefresh(String token) {
    try {
      SignedJWT signedJwt = SignedJWT.parse(token);

      // Valida a assinatura do token
      JWSVerifier verifier = new MACVerifier(jwtProperties.getSecret().getBytes());
      if (!signedJwt.verify(verifier)) {
        throw new ForbiddenSessionException("Sessão de usuário inválida (possível fraude)");
      }

      // Valida o tempo de expiração do token
      JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
      Date expiration = claims.getExpirationTime();
      if (expiration == null || expiration.before(new Date())) {
        throw new ForbiddenSessionException(
            String.format("Sessão expirada para o usuário '%s'", claims.getSubject()));
      }

      // Valida login do token
      String login = claims.getSubject();
      var user = userService.findByLogin(login)
          .orElseThrow(
              () -> new UserNotFoundException(String.format("Usuário não encontrado para o login '%s'", login)));

      // Valida jwtId do token
      String jwtId = claims.getJWTID();
      var checkJwtId = getJwtId(user.getId());
      if (!jwtId.equals(checkJwtId)) {
        throw new ForbiddenSessionException("Sessão de usuário inválida (possível fraude)");
      }

      // Gera um novo token com tempo de expiração renovado
      String refreshedToken = generateSessionToken(login, checkJwtId);

      log.debug("Token de sessão renovado para o usuário");
      return new SessionClaims(login, refreshedToken);

    } catch (ForbiddenSessionException e) {
      throw e;
    } catch (Exception e) {
      throw new InfrastructureException("Erro ao validar sessão do usuário", e);
    }
  }

  /**
   * Gera um jwtId único para o usuário.
   * 
   * @param userId Id do usuário
   * @return jwtId único para o usuário
   */
  public String getJwtId(Long userId) {
    log.debug("Gerando jwtId para o usuário {} -> {}", userId, Long.reverseBytes(userId));
    return CryptUtils.encrypt("" + Long.reverseBytes(userId));
  }
}
