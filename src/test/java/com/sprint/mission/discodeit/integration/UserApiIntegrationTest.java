package com.sprint.mission.discodeit.integration;

import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserApiIntegrationTest extends IntegrationTestSupport {

  // 유저 생성 성공
  @Test
  @DisplayName("유저 생성 API 통합 테스트를 검증한다.")
  void create_success() throws Exception {
    // given
    UserCreateRequest request = new UserCreateRequest("alpha", "alpha@example.com", "password123");

    // when
    ResultActions result = mockMvc.perform(multipart("/api/users")
        .file(jsonPart("userCreateRequest", request)));

    // then
    result
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("alpha"))
        .andExpect(jsonPath("$.email").value("alpha@example.com"));

    flushAndClear();
    User savedUser = userRepository.findByUsername("alpha").orElseThrow();

    assertThat(savedUser.getEmail()).isEqualTo("alpha@example.com");
    assertThat(savedUser.getStatus()).isNotNull();
  }

  // 유저 수정 성공
  @Test
  @DisplayName("유저 수정 API 통합 테스트를 검증한다.")
  void update_success() throws Exception {
    // given
    User savedUser = persistUser("alpha", "alpha@example.com");
    UserUpdateRequest request = new UserUpdateRequest("bravo", "bravo@example.com", "newpassword123");

    // when
    ResultActions result = mockMvc.perform(multipartPatch("/api/users/{userId}", savedUser.getId())
        .file(jsonPart("userUpdateRequest", request)));

    // then
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("bravo"))
        .andExpect(jsonPath("$.email").value("bravo@example.com"));

    flushAndClear();
    User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();

    assertThat(updatedUser.getUsername()).isEqualTo("bravo");
    assertThat(updatedUser.getEmail()).isEqualTo("bravo@example.com");
    assertThat(updatedUser.getPassword()).isEqualTo("newpassword123");
  }

  // 유저 삭제 성공
  @Test
  @DisplayName("유저 삭제 API 통합 테스트를 검증한다.")
  void delete_success() throws Exception {
    // given
    User savedUser = persistUser("alpha", "alpha@example.com");

    // when
    ResultActions result = mockMvc.perform(delete("/api/users/{userId}", savedUser.getId()));

    // then
    result.andExpect(status().isNoContent());

    flushAndClear();
    assertThat(userRepository.existsById(savedUser.getId())).isFalse();
  }

  // 전체 유저 조회 성공
  @Test
  @DisplayName("전체 유저 조회 API 통합 테스트를 검증한다.")
  void find_all_success() throws Exception {
    // given
    persistUser("alpha", "alpha@example.com");
    persistUser("bravo", "bravo@example.com");

    // when
    ResultActions result = mockMvc.perform(get("/api/users"));

    // then
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].username", containsInAnyOrder("alpha", "bravo")))
        .andExpect(jsonPath("$[*].email", containsInAnyOrder("alpha@example.com", "bravo@example.com")));
  }
}
