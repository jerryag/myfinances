package br.com.infotech.myfinances.filter;

import br.com.infotech.myfinances.context.UserContext;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import br.com.infotech.myfinances.util.MdcUtils;
import br.com.infotech.myfinances.domain.MdcKey;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String userLogin = request.getHeader("X-User-Login");

    // Injeta TraceId e UserLogin na memória MDC do logback atual
    MdcUtils.defineTraceId();
    MdcUtils.set(MdcKey.USER_LOGIN, StringUtils.isNotBlank(userLogin) ? userLogin : "-");

    if (StringUtils.isNotBlank(userLogin)) {
      Optional<User> userOptional = userRepository.findByLogin(userLogin);
      if (userOptional.isPresent()) {
        UserContext.setCurrentUser(userOptional.get());
      }
    }

    try {
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      throw e; // Lançar limpo: Advices tratarão
    } finally {
      UserContext.clear();
      MdcUtils.clear();
    }
  }
}
