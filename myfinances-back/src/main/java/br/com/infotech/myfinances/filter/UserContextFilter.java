package br.com.infotech.myfinances.filter;

import br.com.infotech.myfinances.context.UserContext;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String userLogin = request.getHeader("X-User-Login");
    log.info("Processing request: {} {}. User Login Header: {}", request.getMethod(), request.getRequestURI(), userLogin);

    if (StringUtils.isNotBlank(userLogin)) {
      Optional<User> userOptional = userRepository.findByLogin(userLogin);
      if (userOptional.isPresent()) {
        UserContext.setCurrentUser(userOptional.get());
        log.info("User context set for login: {}", userLogin);
      } else {
        log.warn("User not found for login: {}", userLogin);
      }
    } else {
      log.info("No X-User-Login header found");
    }

    try {
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      log.error("Error in filter chain", e);
      throw e;
    } finally {
      UserContext.clear();
    }
  }
}
