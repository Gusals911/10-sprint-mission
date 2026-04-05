package com.sprint.mission.discodeit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
class UserControllerSliceTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;
  @MockitoBean
  private UserStatusService userStatusService;

  @Test
  @DisplayName("create returns the created user as JSON")
  void create_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    BinaryContentDto profile = new BinaryContentDto(UUID.randomUUID(), "profile.png", 4L, "image/png");
    UserDto expected = new UserDto(userId, "alpha", "alpha@example.com", profile, false);
    UserCreateRequest request = new UserCreateRequest("alpha", "alpha@example.com", "password123");
    MockMultipartFile requestPart = new MockMultipartFile(
        "userCreateRequest",
        "",
        "application/json",
        objectMapper.writeValueAsBytes(request)
    );
    MockMultipartFile profilePart = new MockMultipartFile(
        "profile",
        "profile.png",
        "image/png",
        "test".getBytes(StandardCharsets.UTF_8)
    );

    given(userService.create(any(), any())).willReturn(expected);

    // when & then
    mockMvc.perform(multipart("/api/users")
            .file(requestPart)
            .file(profilePart))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.username").value("alpha"))
        .andExpect(jsonPath("$.email").value("alpha@example.com"))
        .andExpect(jsonPath("$.profile.fileName").value("profile.png"))
        .andExpect(jsonPath("$.online").value(false));

    then(userService).should().create(any(), any());
  }

  @Test
  @DisplayName("delete returns not found JSON when the user does not exist")
  void delete_fail() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    willThrow(new UserNotFoundException(userId))
        .given(userService)
        .delete(userId);

    // when & then
    mockMvc.perform(delete("/api/users/{userId}", userId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("U003"))
        .andExpect(jsonPath("$.exceptionType").value("UserNotFoundException"))
        .andExpect(jsonPath("$.details.userId").value(userId.toString()))
        .andExpect(jsonPath("$.status").value(404));
  }
}
