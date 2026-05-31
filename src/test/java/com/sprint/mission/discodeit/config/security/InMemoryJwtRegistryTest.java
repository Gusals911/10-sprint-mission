package com.sprint.mission.discodeit.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.entity.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryJwtRegistryTest {

  @Test
  void registerJwtInformation_LimitsActiveJwtCount() {
    InMemoryJwtRegistry jwtRegistry = new InMemoryJwtRegistry(1);
    UUID userId = UUID.randomUUID();
    JwtInformation first = jwtInformation(userId, "access-1", "refresh-1");
    JwtInformation second = jwtInformation(userId, "access-2", "refresh-2");

    jwtRegistry.registerJwtInformation(first);
    int removedCount = jwtRegistry.registerJwtInformation(second);

    assertThat(removedCount).isEqualTo(1);
    assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access-1")).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-1")).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access-2")).isTrue();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-2")).isTrue();
  }

  @Test
  void rotateJwtInformation_InvalidatesOldRefreshToken() {
    InMemoryJwtRegistry jwtRegistry = new InMemoryJwtRegistry(1);
    UUID userId = UUID.randomUUID();
    jwtRegistry.registerJwtInformation(jwtInformation(userId, "access-1", "refresh-1"));

    boolean rotated = jwtRegistry.rotateJwtInformation(
        "refresh-1",
        jwtInformation(userId, "access-2", "refresh-2")
    );

    assertThat(rotated).isTrue();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-1")).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access-2")).isTrue();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-2")).isTrue();
  }

  @Test
  void invalidateJwtInformationByUserId_RemovesUserTokens() {
    InMemoryJwtRegistry jwtRegistry = new InMemoryJwtRegistry(1);
    UUID userId = UUID.randomUUID();
    jwtRegistry.registerJwtInformation(jwtInformation(userId, "access", "refresh"));

    int removedCount = jwtRegistry.invalidateJwtInformationByUserId(userId);

    assertThat(removedCount).isEqualTo(1);
    assertThat(jwtRegistry.hasActiveJwtInformationByUserId(userId)).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access")).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh")).isFalse();
  }

  private JwtInformation jwtInformation(UUID userId, String accessToken, String refreshToken) {
    Instant now = Instant.now();
    return new JwtInformation(
        userId,
        "testuser",
        Role.USER,
        accessToken,
        refreshToken,
        now.plusSeconds(60),
        now.plusSeconds(120)
    );
  }
}
