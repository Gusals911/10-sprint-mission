package com.sprint.mission.discodeit.integration;

import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ChannelApiIntegrationTest extends IntegrationTestSupport {

  // 공개 채널 생성 성공
  @Test
  @DisplayName("공개 채널 생성 API 통합 테스트를 검증한다.")
  void create_public_success() throws Exception {
    // given
    PublicChannelCreateRequest request = new PublicChannelCreateRequest("backend", "backend channel");

    // when
    ResultActions result = mockMvc.perform(post("/api/channels/public")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)));

    // then
    result
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("PUBLIC"))
        .andExpect(jsonPath("$.name").value("backend"))
        .andExpect(jsonPath("$.description").value("backend channel"))
        .andExpect(jsonPath("$.participants", hasSize(0)));

    flushAndClear();
    assertThat(channelRepository.findAll()).hasSize(1);
    assertThat(channelRepository.findAll().get(0).getName()).isEqualTo("backend");
  }

  // 비공개 채널 생성 성공
  @Test
  @DisplayName("비공개 채널 생성 API 통합 테스트를 검증한다.")
  void create_private_success() throws Exception {
    // given
    User firstUser = persistUser("alpha", "alpha@example.com");
    User secondUser = persistUser("bravo", "bravo@example.com");
    PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(
        List.of(firstUser.getId(), secondUser.getId()));

    // when
    ResultActions result = mockMvc.perform(post("/api/channels/private")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)));

    // then
    result
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("PRIVATE"))
        .andExpect(jsonPath("$.participants", hasSize(2)))
        .andExpect(jsonPath("$.participants[*].username", containsInAnyOrder("alpha", "bravo")));

    flushAndClear();
    Channel savedChannel = channelRepository.findAll().get(0);
    assertThat(readStatusRepository.findAllByChannelIdWithUser(savedChannel.getId())).hasSize(2);
  }

  // 채널 수정 성공
  @Test
  @DisplayName("채널 수정 API 통합 테스트를 검증한다.")
  void update_success() throws Exception {
    // given
    Channel savedChannel = persistPublicChannel("backend", "backend channel");
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("frontend", "frontend channel");

    // when
    ResultActions result = mockMvc.perform(patch("/api/channels/{channelId}", savedChannel.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)));

    // then
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("frontend"))
        .andExpect(jsonPath("$.description").value("frontend channel"));

    flushAndClear();
    Channel updatedChannel = channelRepository.findById(savedChannel.getId()).orElseThrow();

    assertThat(updatedChannel.getName()).isEqualTo("frontend");
    assertThat(updatedChannel.getDescription()).isEqualTo("frontend channel");
  }

  // 채널 삭제 성공
  @Test
  @DisplayName("채널 삭제 API 통합 테스트를 검증한다.")
  void delete_success() throws Exception {
    // given
    Channel savedChannel = persistPublicChannel("backend", "backend channel");

    // when
    ResultActions result = mockMvc.perform(delete("/api/channels/{channelId}", savedChannel.getId()));

    // then
    result.andExpect(status().isNoContent());

    flushAndClear();
    assertThat(channelRepository.existsById(savedChannel.getId())).isFalse();
  }

  // 비공개 채널 수정 실패
  @Test
  @DisplayName("비공개 채널 수정 시 에러 응답을 검증한다.")
  void update_private_fail() throws Exception {
    // given
    Channel savedChannel = persistPrivateChannel();
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("frontend", "frontend channel");

    // when
    ResultActions result = mockMvc.perform(patch("/api/channels/{channelId}", savedChannel.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)));

    // then
    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("C002"))
        .andExpect(jsonPath("$.details.channelId").value(savedChannel.getId().toString()));
  }
}
