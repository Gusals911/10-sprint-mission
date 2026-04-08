package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositorySliceTest {

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private UserStatusRepository userStatusRepository;
  @Autowired
  private BinaryContentRepository binaryContentRepository;
  @Autowired
  private TestEntityManager entityManager;

  // username으로 유저 조회 성공
  @Test
  @DisplayName("username으로 유저 조회를 검증한다.")
  void find_by_username_success() {
    // given
    User savedUser = persistUser("alpha", "alpha@example.com", true);

    // when
    Optional<User> result = userRepository.findByUsername("alpha");

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(savedUser.getId());
    assertThat(result.get().getEmail()).isEqualTo("alpha@example.com");
  }

  // username으로 유저 조회 실패
  @Test
  @DisplayName("존재하지 않는 username으로 조회하면 빈 값을 반환한다.")
  void find_by_username_fail() {
    // when
    Optional<User> result = userRepository.findByUsername("missing");

    // then
    assertThat(result).isEmpty();
  }

  // profile, status 포함 전체 유저 조회 성공
  @Test
  @DisplayName("profile과 status를 포함한 전체 유저 조회를 검증한다.")
  void find_all_with_profile_and_status_success() {
    // given
    User savedUser = persistUser("bravo", "bravo@example.com", true);

    // when
    List<User> result = userRepository.findAllWithProfileAndStatus();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(savedUser.getId());
    assertThat(result.get(0).getProfile()).isNotNull();
    assertThat(result.get(0).getStatus()).isNotNull();
    assertThat(result.get(0).getStatus().getUser().getId()).isEqualTo(savedUser.getId());
  }

  // profile, status 포함 전체 유저 조회 실패
  @Test
  @DisplayName("저장된 유저가 없으면 빈 목록을 반환한다.")
  void find_all_with_profile_and_status_fail() {
    // when
    List<User> result = userRepository.findAllWithProfileAndStatus();

    // then
    assertThat(result).isEmpty();
  }

  private User persistUser(String username, String email, boolean withProfile) {
    BinaryContent profile = null;
    if (withProfile) {
      profile = binaryContentRepository.saveAndFlush(
          new BinaryContent("profile.png", 4L, "image/png"));
    }

    User user = userRepository.save(new User(username, email, "password123", profile));
    userStatusRepository.save(new UserStatus(user, Instant.parse("2026-04-05T00:00:00Z")));

    entityManager.flush();
    entityManager.clear();
    return user;
  }
}
