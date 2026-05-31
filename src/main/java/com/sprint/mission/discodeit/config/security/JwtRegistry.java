package com.sprint.mission.discodeit.config.security;

import java.util.UUID;

public interface JwtRegistry {

  int registerJwtInformation(JwtInformation jwtInformation);

  int invalidateJwtInformationByUserId(UUID userId);

  int invalidateJwtInformationByRefreshToken(String refreshToken);

  boolean hasActiveJwtInformationByUserId(UUID userId);

  boolean hasActiveJwtInformationByAccessToken(String accessToken);

  boolean hasActiveJwtInformationByRefreshToken(String refreshToken);

  boolean rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation);

  void clearExpiredJwtInformation();
}
