package com.sprint.mission.discodeit.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.DefaultCsrfToken;

class AuthControllerTest {

  @Test
  @DisplayName("CSRF 토큰 요청 시 203 상태를 반환한다")
  void getCsrfToken_ReturnsNonAuthoritativeInformation() {
    AuthController authController = new AuthController();
    DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "test-token");

    ResponseEntity<Void> response = authController.getCsrfToken(csrfToken);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NON_AUTHORITATIVE_INFORMATION);
  }
}
