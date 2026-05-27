package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.config.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.config.security.JwtTokenProvider;
import com.sprint.mission.discodeit.dto.data.JwtDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.UserRoleUpdateRequest;
import com.sprint.mission.discodeit.exception.ErrorResponse;
import com.sprint.mission.discodeit.service.UserService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

  private final UserService userService;
  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsService userDetailsService;

  @GetMapping(path = "csrf-token")
  public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
    String tokenValue = csrfToken.getToken();
    log.debug("CSRF 토큰 요청: {}", tokenValue);

    return ResponseEntity
        .status(HttpStatus.NON_AUTHORITATIVE_INFORMATION)
        .build();
  }

  @PostMapping(path = "refresh")
  public ResponseEntity<?> refresh(
      @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
  ) {
    if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
      return unauthorizedRefreshTokenResponse();
    }

    try {
      String username = jwtTokenProvider.getUsername(refreshToken);
      DiscodeitUserDetails userDetails =
          (DiscodeitUserDetails) userDetailsService.loadUserByUsername(username);

      String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
      String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

      ResponseCookie refreshTokenCookie = createRefreshTokenCookie(newRefreshToken);
      JwtDto jwtDto = new JwtDto(userDetails.toAuthenticatedUserDto(), newAccessToken);

      return ResponseEntity
          .status(HttpStatus.OK)
          .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
          .body(jwtDto);
    } catch (Exception e) {
      return unauthorizedRefreshTokenResponse();
    }
  }

  @PutMapping(path = "role")
  public ResponseEntity<UserDto> updateRole(
      @RequestBody @Valid UserRoleUpdateRequest request
  ) {
    UserDto userDto = userService.updateRole(request.userId(), request.newRole());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(userDto);
  }

  private ResponseEntity<ErrorResponse> unauthorizedRefreshTokenResponse() {
    ErrorResponse response = new ErrorResponse(
        Instant.now(),
        "INVALID_REFRESH_TOKEN",
        "유효하지 않은 리프레시 토큰입니다.",
        Map.of(),
        "InvalidRefreshToken",
        HttpStatus.UNAUTHORIZED.value()
    );

    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(response);
  }

  private ResponseCookie createRefreshTokenCookie(String refreshToken) {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Strict")
        .maxAge(Duration.ofMinutes(jwtTokenProvider.getRefreshTokenExpirationMinutes()))
        .build();
  }
}
