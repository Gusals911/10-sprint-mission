package com.sprint.mission.discodeit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sprint.mission.discodeit.config.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.config.security.JwtRegistry;
import com.sprint.mission.discodeit.config.security.JwtTokenProvider;
import com.sprint.mission.discodeit.dto.data.JwtDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.exception.ErrorResponse;
import com.sprint.mission.discodeit.service.UserService;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.csrf.DefaultCsrfToken;

class AuthControllerTest {

  @Test
  @DisplayName("CSRF 토큰 요청 시 203 상태를 반환한다")
  void getCsrfToken_ReturnsNonAuthoritativeInformation() {
    AuthController authController = authController(mock(JwtTokenProvider.class), mock(JwtRegistry.class),
        mock(UserDetailsService.class));
    DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "test-token");

    ResponseEntity<Void> response = authController.getCsrfToken(csrfToken);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NON_AUTHORITATIVE_INFORMATION);
  }

  @Test
  @DisplayName("리프레시 토큰이 없으면 401 ErrorResponse를 반환한다")
  void refresh_ReturnsUnauthorized_WhenRefreshTokenMissing() {
    AuthController authController = authController(mock(JwtTokenProvider.class), mock(JwtRegistry.class),
        mock(UserDetailsService.class));

    ResponseEntity<?> response = authController.refresh(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
  }

  @Test
  @DisplayName("유효한 리프레시 토큰이면 새 토큰을 발급한다")
  void refresh_ReturnsJwtDto_WhenRefreshTokenValid() {
    JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    JwtRegistry jwtRegistry = mock(JwtRegistry.class);
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    AuthController authController = authController(jwtTokenProvider, jwtRegistry, userDetailsService);
    String refreshToken = "refresh-token";
    String newAccessToken = "new-access-token";
    String newRefreshToken = "new-refresh-token";
    UserDto userDto = new UserDto(
        UUID.randomUUID(),
        "testuser",
        "test@example.com",
        null,
        true
    );
    DiscodeitUserDetails userDetails = new DiscodeitUserDetails(userDto, "encoded-password");

    when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
    when(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).thenReturn(true);
    when(jwtTokenProvider.getUsername(refreshToken)).thenReturn("testuser");
    when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
    when(jwtTokenProvider.generateAccessToken(userDetails)).thenReturn(newAccessToken);
    when(jwtTokenProvider.generateRefreshToken(userDetails)).thenReturn(newRefreshToken);
    when(jwtTokenProvider.getExpirationTime(newAccessToken))
        .thenReturn(Date.from(Instant.now().plusSeconds(60)));
    when(jwtTokenProvider.getExpirationTime(newRefreshToken))
        .thenReturn(Date.from(Instant.now().plusSeconds(120)));
    when(jwtTokenProvider.getRefreshTokenExpirationMinutes()).thenReturn(10080);
    when(jwtRegistry.rotateJwtInformation(org.mockito.ArgumentMatchers.eq(refreshToken),
        org.mockito.ArgumentMatchers.any())).thenReturn(true);

    ResponseEntity<?> response = authController.refresh(refreshToken);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
        .contains("REFRESH_TOKEN=" + newRefreshToken);
    assertThat(response.getBody()).isEqualTo(new JwtDto(userDetails.toAuthenticatedUserDto(),
        newAccessToken));
  }

  private AuthController authController(
      JwtTokenProvider jwtTokenProvider,
      JwtRegistry jwtRegistry,
      UserDetailsService userDetailsService
  ) {
    return new AuthController(mock(UserService.class), jwtTokenProvider, jwtRegistry, userDetailsService);
  }
}
