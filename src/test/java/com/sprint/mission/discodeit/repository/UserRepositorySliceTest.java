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
class UserRepositorySliceTest {

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private UserStatusRepository userStatusRepository;
  @Autowired
  private BinaryContentRepository binaryContentRepository;
  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("findByUsername returns a user when the username exists")
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

  @Test
  @DisplayName("findByUsername returns empty when the username does not exist")
  void find_by_username_fail() {
    // when
    Optional<User> result = userRepository.findByUsername("missing");

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findAllWithProfileAndStatus returns users with profile and status")
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

  @Test
  @DisplayName("findAllWithProfileAndStatus returns an empty list when no users exist")
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
