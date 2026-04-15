package br.com.infotech.myfinances.filter;

import br.com.infotech.myfinances.context.UserContext;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.exception.ForbiddenSessionException;
import br.com.infotech.myfinances.service.JwtSessionService;
import br.com.infotech.myfinances.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import br.com.infotech.myfinances.util.MdcUtils;
import br.com.infotech.myfinances.domain.MdcKey;

@Component
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

  private final UserService userService;
  private final JwtSessionService jwtSessionService;
  private final HandlerExceptionResolver resolver;

  public UserContextFilter(
      UserService userService,
      JwtSessionService jwtSessionService,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.userService = userService;
    this.jwtSessionService = jwtSessionService;
    this.resolver = resolver;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return "/login".equals(path);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String sessionToken = request.getHeader("X-Session-Token");

    MdcUtils.defineTraceId();

    try {
      // Se possui token de sessão de usuário, então validar e renovar, se válida
      if (StringUtils.isNotBlank(sessionToken)) {
        JwtSessionService.SessionClaims claims = jwtSessionService.validateAndRefresh(sessionToken);
        String userLogin = claims.login();
        MdcUtils.set(MdcKey.USER_LOGIN, userLogin);

        Optional<User> userOptional = userService.findByLogin(userLogin);
        if (userOptional.isPresent()) {
          UserContext.setCurrentUser(userOptional.get());
          // Devolve o token renovado no header da resposta
          response.setHeader("X-Session-Token", claims.refreshedToken());
        }
      } else {
        MdcUtils.set(MdcKey.USER_LOGIN, "-");
        throw new ForbiddenSessionException("Sessão de usuário ausente ou expirada. Faça login novamente.");
      }

      filterChain.doFilter(request, response);

    } catch (ForbiddenSessionException e) {
      MdcUtils.set(MdcKey.USER_LOGIN, "-");
      resolver.resolveException(request, response, null, e);
    } catch (Exception e) {
      resolver.resolveException(request, response, null, e);
    }
  }
}
