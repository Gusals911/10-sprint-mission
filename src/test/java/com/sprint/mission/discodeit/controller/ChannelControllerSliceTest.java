package com.sprint.mission.discodeit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.service.ChannelService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChannelController.class)
@ActiveProfiles("test")
class ChannelControllerSliceTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ChannelService channelService;

  @Test
  @DisplayName("create public channel returns the created channel as JSON")
  void create_public_success() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    PublicChannelCreateRequest request = new PublicChannelCreateRequest("backend", "backend channel");
    ChannelDto expected = new ChannelDto(
        channelId,
        ChannelType.PUBLIC,
        "backend",
        "backend channel",
        List.of(),
        Instant.parse("2026-04-05T10:00:00Z")
    );

    given(channelService.create(any(PublicChannelCreateRequest.class))).willReturn(expected);

    // when & then
    mockMvc.perform(post("/api/channels/public")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(channelId.toString()))
        .andExpect(jsonPath("$.type").value("PUBLIC"))
        .andExpect(jsonPath("$.name").value("backend"))
        .andExpect(jsonPath("$.description").value("backend channel"))
        .andExpect(jsonPath("$.lastMessageAt").value("2026-04-05T10:00:00Z"));

    then(channelService).should().create(any(PublicChannelCreateRequest.class));
  }

  @Test
  @DisplayName("update returns not found JSON when the channel does not exist")
  void update_fail() throws Exception {
    // given
    UUID channelId = UUID.randomUUID();
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("frontend", "frontend channel");

    willThrow(new ChannelNotFoundException(channelId))
        .given(channelService)
        .update(eq(channelId), any(PublicChannelUpdateRequest.class));

    // when & then
    mockMvc.perform(patch("/api/channels/{channelId}", channelId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("C001"))
        .andExpect(jsonPath("$.exceptionType").value("ChannelNotFoundException"))
        .andExpect(jsonPath("$.details.channelId").value(channelId.toString()))
        .andExpect(jsonPath("$.status").value(404));
  }
}
