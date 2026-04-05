package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.exception.message.MessageNotFoundException;
import com.sprint.mission.discodeit.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@ActiveProfiles("test")
public class MessageControllerSliceTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private MessageService messageService;

  // 채널별 메시지 조회 성공
  @Test
  @DisplayName("채널별 메시지 조회 응답 JSON을 검증한다.")
  void find_all_by_channel_id_success() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    UUID messageId = UUID.randomUUID();
    Instant cursor = Instant.parse("2026-04-05T10:00:00Z");
    UserDto author = new UserDto(UUID.randomUUID(), "alpha", "alpha@example.com", null, false);
    MessageDto message = new MessageDto(
        messageId,
        Instant.parse("2026-04-05T09:59:00Z"),
        Instant.parse("2026-04-05T09:59:30Z"),
        "hello",
        channelId,
        author,
        List.of()
    );
    PageResponse<MessageDto> expected = new PageResponse<>(
        List.of(message),
        cursor,
        2,
        false,
        null
    );

    given(messageService.findAllByChannelId(eq(channelId), eq(cursor), any())).willReturn(expected);

    // when
    ResultActions result = mockMvc.perform(get("/api/messages")
        .param("channelId", channelId.toString())
        .param("cursor", cursor.toString())
        .param("page", "0")
        .param("size", "2")
        .param("sort", "createdAt,desc"));

    // then
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(messageId.toString()))
        .andExpect(jsonPath("$.content[0].content").value("hello"))
        .andExpect(jsonPath("$.content[0].author.username").value("alpha"))
        .andExpect(jsonPath("$.nextCursor").value("2026-04-05T10:00:00Z"))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.hasNext").value(false));

    then(messageService).should().findAllByChannelId(eq(channelId), eq(cursor), any());
  }

  // 메시지 수정 실패
  @Test
  @DisplayName("존재하지 않는 메시지 수정 시 에러 응답 JSON을 검증한다.")
  void update_fail() throws Exception {
    // given
    UUID messageId = UUID.randomUUID();
    MessageUpdateRequest request = new MessageUpdateRequest("updated");

    willThrow(new MessageNotFoundException(messageId))
        .given(messageService)
        .update(eq(messageId), any(MessageUpdateRequest.class));

    // when
    ResultActions result = mockMvc.perform(patch("/api/messages/{messageId}", messageId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)));

    // then
    result
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("M001"))
        .andExpect(jsonPath("$.exceptionType").value("MessageNotFoundException"))
        .andExpect(jsonPath("$.details.messageId").value(messageId.toString()))
        .andExpect(jsonPath("$.status").value(404));
  }
}
