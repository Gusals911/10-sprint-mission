package com.sprint.mission.discodeit.config.security;

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InMemoryJwtRegistry implements JwtRegistry {

  private final Map<UUID, Queue<JwtInformation>> origin = new ConcurrentHashMap<>();
  private final int maxActiveJwtCount;

  public InMemoryJwtRegistry(
      @Value("${jwt.max-active-count:1}") int maxActiveJwtCount
  ) {
    this.maxActiveJwtCount = maxActiveJwtCount;
  }

  @Override
  public synchronized int registerJwtInformation(JwtInformation jwtInformation) {
    clearExpiredJwtInformation();

    Queue<JwtInformation> jwtInformations = origin.computeIfAbsent(
        jwtInformation.userId(),
        userId -> new ConcurrentLinkedQueue<>()
    );
    jwtInformations.add(jwtInformation);

    int removedCount = 0;
    while (jwtInformations.size() > maxActiveJwtCount) {
      if (jwtInformations.poll() != null) {
        removedCount++;
      }
    }

    return removedCount;
  }

  @Override
  public synchronized int invalidateJwtInformationByUserId(UUID userId) {
    Queue<JwtInformation> removed = origin.remove(userId);
    return removed == null ? 0 : removed.size();
  }

  @Override
  public synchronized int invalidateJwtInformationByRefreshToken(String refreshToken) {
    int removedCount = 0;

    for (Queue<JwtInformation> jwtInformations : origin.values()) {
      int beforeSize = jwtInformations.size();
      jwtInformations.removeIf(jwtInformation -> jwtInformation.hasRefreshToken(refreshToken));
      removedCount += beforeSize - jwtInformations.size();
    }
    origin.entrySet().removeIf(entry -> entry.getValue().isEmpty());

    return removedCount;
  }

  @Override
  public boolean hasActiveJwtInformationByUserId(UUID userId) {
    Queue<JwtInformation> jwtInformations = origin.get(userId);
    if (jwtInformations == null) {
      return false;
    }

    Instant now = Instant.now();
    return jwtInformations.stream()
        .anyMatch(jwtInformation -> jwtInformation.isRefreshTokenActive(now));
  }

  @Override
  public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
    Instant now = Instant.now();
    return origin.values().stream()
        .flatMap(Queue::stream)
        .anyMatch(jwtInformation ->
            jwtInformation.hasAccessToken(accessToken)
                && jwtInformation.isAccessTokenActive(now)
        );
  }

  @Override
  public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
    Instant now = Instant.now();
    return origin.values().stream()
        .flatMap(Queue::stream)
        .anyMatch(jwtInformation ->
            jwtInformation.hasRefreshToken(refreshToken)
                && jwtInformation.isRefreshTokenActive(now)
        );
  }

  @Override
  public synchronized boolean rotateJwtInformation(
      String refreshToken,
      JwtInformation newJwtInformation
  ) {
    int removedCount = invalidateJwtInformationByRefreshToken(refreshToken);
    if (removedCount == 0) {
      return false;
    }

    registerJwtInformation(newJwtInformation);
    return true;
  }

  @Scheduled(fixedDelay = 1000 * 60 * 5)
  @Override
  public synchronized void clearExpiredJwtInformation() {
    Instant now = Instant.now();

    origin.values().forEach(jwtInformations ->
        jwtInformations.removeIf(jwtInformation -> !jwtInformation.isRefreshTokenActive(now))
    );
    origin.entrySet().removeIf(entry -> entry.getValue().isEmpty());
  }
}
