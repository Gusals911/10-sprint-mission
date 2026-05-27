package com.sprint.mission.discodeit.config.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final String REMEMBER_ME_KEY = "discodeit-remember-me";
  private static final int REMEMBER_ME_VALIDITY_SECONDS = 60 * 60 * 24 * 14;

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      LoginSuccessHandler loginSuccessHandler,
      LoginFailureHandler loginFailureHandler,
      SessionRegistry sessionRegistry,
      UserDetailsService userDetailsService
  ) throws Exception {
    http
        // CSR 방식에서 JavaScript가 CSRF 토큰 쿠키를 읽을 수 있도록 HttpOnly를 해제
        .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            // CSRF 토큰 지연 로딩 문제를 해결하기 위해 커스텀 핸들러 사용
            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
        .authorizeHttpRequests(auth -> auth
            // 인증 없이 접근해야 하는 인증 준비 요청
            .requestMatchers(HttpMethod.GET, "/api/auth/csrf-token").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
            // 프론트 정적 리소스와 API 문서, 모니터링 요청은 인증 없이 허용
            .requestMatchers("/", "/index.html", "/favicon.ico", "/assets/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            // 권한 수정 요청은 ADMIN 권한 필요
            .requestMatchers(HttpMethod.PUT, "/api/auth/role").hasRole("ADMIN")
            // 그 외 모든 요청은 인증(로그인) 필요
            .anyRequest().authenticated())
        .exceptionHandling(exception -> exception
            // 인증되지 않은 사용자는 401 응답
            .authenticationEntryPoint((request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
            // 인증은 되었지만 권한이 부족한 사용자는 403 응답
            .accessDeniedHandler((request, response, accessDeniedException) ->
                response.sendError(HttpServletResponse.SC_FORBIDDEN)))
        .sessionManagement(management -> management
            .sessionConcurrency(concurrency -> concurrency
                // 동일 계정으로 유지할 수 있는 로그인 세션을 1개로 제한
                .maximumSessions(1)
                // Remember-me 재인증처럼 새 인증이 들어오면 기존 세션을 만료하고 새 세션을 허용
                .maxSessionsPreventsLogin(false)
                // 동시 로그인 제한과 강제 세션 만료 처리에서 같은 세션 저장소를 사용
                .sessionRegistry(sessionRegistry)))
        // 로그인 필터는 Spring Security 기본 흐름을 사용하고 성공/실패 응답만 커스터마이징
        .formLogin(login -> login
            .loginProcessingUrl("/api/auth/login")
            .successHandler(loginSuccessHandler)
            .failureHandler(loginFailureHandler))
        .logout(logout -> logout
            .logoutUrl("/api/auth/logout")
            .logoutSuccessHandler(
                new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
        .rememberMe(rememberMe -> rememberMe
            // 프론트 로그인 유지 체크박스가 전달하는 요청 파라미터 이름
            .rememberMeParameter("remember-me")
            .key(REMEMBER_ME_KEY)
            .tokenValiditySeconds(REMEMBER_ME_VALIDITY_SECONDS)
            .userDetailsService(userDetailsService));

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SessionRegistry sessionRegistry() {
    // 현재 로그인한 Principal과 세션 정보를 추적
    return new SessionRegistryImpl();
  }

  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    // HttpSession 만료/소멸 이벤트를 SessionRegistry에 반영
    return new HttpSessionEventPublisher();
  }

  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy("""
        ROLE_ADMIN > ROLE_CHANNEL_MANAGER
        ROLE_CHANNEL_MANAGER > ROLE_USER
        """);
  }

  @Bean
  static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
      RoleHierarchy roleHierarchy
  ) {
    DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
    // 메서드 권한 검사에서도 권한 계층을 반영
    handler.setRoleHierarchy(roleHierarchy);
    return handler;
  }
}
