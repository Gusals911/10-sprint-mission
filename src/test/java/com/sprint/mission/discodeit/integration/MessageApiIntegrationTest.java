package com.sprint.mission.discodeit.integration;

import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import java.time.Instant;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MessageApiIntegrationTest extends IntegrationTestSupport {

  // 메시지 생성 성공
  @Test
  @DisplayName("메시지 생성 API 통합 테스트를 검증한다.")
  void create_success() throws Exception {
    // given
    User author = persistUser("alpha", "alpha@example.com");
    Channel channel = persistPublicChannel("backend", "backend channel");
    MessageCreateRequest request = new MessageCreateRequest("hello", channel.getId(), author.getId());

    // when
    ResultActions result = mockMvc.perform(multipart("/api/messages")
        .file(jsonPart("messageCreateRequest", request)));

    // then
    result
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("hello"))
        .andExpect(jsonPath("$.channelId").value(channel.getId().toString()))
        .andExpect(jsonPath("$.author.username").value("alpha"))
        .andExpect(jsonPath("$.attachments", hasSize(0)));

    flushAndClear();
    assertThat(messageRepository.findAll()).hasSize(1);
    assertThat(messageRepository.findAll().get(0).getContent()).isEqualTo("hello");
  }

  // 메시지 수정 성공
  @Test
  @DisplayName("메시지 수정 API 통합 테스트를 검증한다.")
  void update_success() throws Exception {
    // given
    User author = persistUser("alpha", "alpha@example.com");
    Channel channel = persistPublicChannel("backend", "backend channel");
    Message savedMessage = persistMessage(channel, author, "hello",
        Instant.parse("2026-04-05T09:58:00Z"));
    MessageUpdateRequest request = new MessageUpdateRequest("updated");

    // when
    ResultActions result = mockMvc.perform(patch("/api/messages/{messageId}", savedMessage.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)));

    // then
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("updated"));

    flushAndClear();
    Message updatedMessage = messageRepository.findById(savedMessage.getId()).orElseThrow();
    assertThat(updatedMessage.getContent()).isEqualTo("updated");
  }

  // 메시지 삭제 성공
  @Test
  @DisplayName("메시지 삭제 API 통합 테스트를 검증한다.")
  void delete_success() throws Exception {
    // given
    User author = persistUser("alpha", "alpha@example.com");
    Channel channel = persistPublicChannel("backend", "backend channel");
    Message savedMessage = persistMessage(channel, author, "hello",
        Instant.parse("2026-04-05T09:58:00Z"));

    // when
    ResultActions result = mockMvc.perform(delete("/api/messages/{messageId}", savedMessage.getId()));

    // then
    result.andExpect(status().isNoContent());

    flushAndClear();
    assertThat(messageRepository.existsById(savedMessage.getId())).isFalse();
  }

  // 채널별 메시지 조회 성공
  @Test
  @DisplayName("채널별 메시지 조회 API 통합 테스트를 검증한다.")
  void find_all_by_channel_id_success() throws Exception {
    // given
    User author = persistUser("alpha", "alpha@example.com");
    Channel channel = persistPublicChannel("backend", "backend channel");
    persistMessage(channel, author, "first",
        Instant.parse("2026-04-05T09:57:00Z"));
    persistMessage(channel, author, "second",
        Instant.parse("2026-04-05T09:58:00Z"));
    persistMessage(channel, author, "third",
        Instant.parse("2026-04-05T09:59:00Z"));
    persistMessage(channel, author, "ignored",
        Instant.parse("2026-04-05T10:05:00Z"));

    // when
    ResultActions result = mockMvc.perform(get("/api/messages")
        .param("channelId", channel.getId().toString())
        .param("cursor", "2026-04-05T10:00:00Z")
        .param("page", "0")
        .param("size", "2")
        .param("sort", "createdAt,desc"));

    // then
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].content").value("third"))
        .andExpect(jsonPath("$.content[1].content").value("second"))
        .andExpect(jsonPath("$.nextCursor").value("2026-04-05T09:58:00Z"))
        .andExpect(jsonPath("$.hasNext").value(true));
  }

  // 존재하지 않는 메시지 수정 실패
  @Test
  @DisplayName("존재하지 않는 메시지 수정 시 에러 응답을 검증한다.")
  void update_fail() throws Exception {
    // given
    UUID messageId = UUID.randomUUID();
    MessageUpdateRequest request = new MessageUpdateRequest("updated");

    // when
    ResultActions result = mockMvc.perform(patch("/api/messages/{messageId}", messageId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)));

    // then
    result
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("M001"))
        .andExpect(jsonPath("$.details.messageId").value(messageId.toString()));
  }
}
