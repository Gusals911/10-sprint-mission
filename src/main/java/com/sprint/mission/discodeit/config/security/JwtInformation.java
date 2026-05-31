package com.sprint.mission.discodeit.config.security;

import com.sprint.mission.discodeit.entity.Role;
import java.time.Instant;
import java.util.UUID;

public record JwtInformation(
    UUID userId,
    String username,
    Role role,
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt
) {

  public boolean hasAccessToken(String token) {
    return accessToken.equals(token);
  }

  public boolean hasRefreshToken(String token) {
    return refreshToken.equals(token);
  }

  public boolean isAccessTokenActive(Instant now) {
    return accessTokenExpiresAt.isAfter(now);
  }

  public boolean isRefreshTokenActive(Instant now) {
    return refreshTokenExpiresAt.isAfter(now);
  }
}
