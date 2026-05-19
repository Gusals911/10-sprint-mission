package com.sprint.mission.discodeit.config.security;

import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_EMAIL = "admin@discodeit.com";
  private static final String ADMIN_PASSWORD = "admin1234!";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  @Override
  public void run(ApplicationArguments args) {
    if (userRepository.existsByRole(Role.ADMIN)) {
      log.debug("ADMIN 계정이 이미 존재합니다.");
      return;
    }

    // ADMIN 계정이 없으면 기본 어드민 계정을 생성
    User admin = new User(ADMIN_USERNAME, ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), null);
    admin.updateRole(Role.ADMIN);
    new UserStatus(admin, Instant.now());

    userRepository.save(admin);
    log.info("ADMIN 계정 초기화 완료: username={}", ADMIN_USERNAME);
  }
}
