package com.sprint.mission.discodeit.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.config.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.dto.data.UserDto;
import java.util.UUID;
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

  @Test
  @DisplayName("현재 사용자 정보 요청 시 인증된 사용자 DTO를 반환한다")
  void me_ReturnsAuthenticatedUser() {
    UserDto userDto = new UserDto(
        UUID.randomUUID(),
        "testuser",
        "test@example.com",
        null,
        true
    );
    DiscodeitUserDetails userDetails = new DiscodeitUserDetails(userDto, "encoded-password");

    ResponseEntity<UserDto> response = authController().me(userDetails);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(userDto);
  }

  private AuthController authController() {
    return new AuthController();
  }
}
