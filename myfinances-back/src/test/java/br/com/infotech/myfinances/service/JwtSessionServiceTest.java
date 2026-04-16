package br.com.infotech.myfinances.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import br.com.infotech.myfinances.config.AppJwtProperties;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.exception.ForbiddenSessionException;
import br.com.infotech.myfinances.exception.UserNotFoundException;
import br.com.infotech.myfinances.service.JwtSessionService.SessionClaims;

@ExtendWith(SpringExtension.class)
class JwtSessionServiceTest {

  @InjectMocks
  private JwtSessionService jwtSessionService;

  @Mock
  private AppJwtProperties jwtProperties;

  @Mock
  private UserService userService;

  private final String secret = "12345678901234567890123456789012";
  private final int expirationMinutes = 10;
  private final String login = "testuser";
  private final Long userId = 1L;
  private String validJwtId;
  private User mockedUser;

  @BeforeEach
  void setUp() {
    when(jwtProperties.getSecret()).thenReturn(secret);
    when(jwtProperties.getExpirationMinutes()).thenReturn(expirationMinutes);

    mockedUser = User.builder().id(userId).login(login).build();
    validJwtId = jwtSessionService.getJwtId(userId);
  }

  @Test
  void testGenerateSessionToken() {
    String token = jwtSessionService.generateSessionToken(login, validJwtId);
    
    assertNotNull(token);
  }

  @Test
  void testValidateAndRefreshWithValidToken() {
    when(userService.findByLogin(login)).thenReturn(Optional.of(mockedUser));

    String token = jwtSessionService.generateSessionToken(login, validJwtId);
    SessionClaims claims = jwtSessionService.validateAndRefresh(token);

    assertNotNull(claims);
    assertEquals(login, claims.login());
    assertNotNull(claims.refreshedToken());
  }

  @Test
  void testValidateAndRefreshWithExpiredToken() throws Exception {
    Instant now = Instant.now();
    Instant expiry = now.minusSeconds(60);

    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .subject(login)
        .jwtID(validJwtId)
        .issueTime(Date.from(now.minusSeconds(120)))
        .expirationTime(Date.from(expiry))
        .build();

    JWSSigner signer = new MACSigner(secret.getBytes());
    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    signedJWT.sign(signer);

    String token = signedJWT.serialize();

    assertThrows(ForbiddenSessionException.class, () -> jwtSessionService.validateAndRefresh(token));
  }

  @Test
  void testValidateAndRefreshWithInvalidSignature() throws Exception {
    JWSSigner signer = new MACSigner("abcdefghijklmnopqrstuvwxyz123456".getBytes());
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .subject(login)
        .jwtID(validJwtId)
        .issueTime(new Date())
        .expirationTime(new Date(System.currentTimeMillis() + 60000))
        .build();
    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    signedJWT.sign(signer);
    
    String invalidToken = signedJWT.serialize();

    assertThrows(ForbiddenSessionException.class, () -> jwtSessionService.validateAndRefresh(invalidToken));
  }

  @Test
  void testValidateAndRefreshUserNotFound() {
    when(userService.findByLogin(login)).thenReturn(Optional.empty());

    String token = jwtSessionService.generateSessionToken(login, validJwtId);

    assertThrows(UserNotFoundException.class, () -> jwtSessionService.validateAndRefresh(token));
  }

  @Test
  void testValidateAndRefreshInvalidJwtId() {
    when(userService.findByLogin(login)).thenReturn(Optional.of(mockedUser));

    String token = jwtSessionService.generateSessionToken(login, "invalid_jwt_id");

    assertThrows(ForbiddenSessionException.class, () -> jwtSessionService.validateAndRefresh(token));
  }

  @Test
  void testGetJwtId() {
    String encrypted = jwtSessionService.getJwtId(userId);
    assertNotNull(encrypted);
  }
}
