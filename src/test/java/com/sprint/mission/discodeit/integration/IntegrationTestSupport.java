package com.sprint.mission.discodeit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

public abstract class IntegrationTestSupport {

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  protected ObjectMapper objectMapper;
  @Autowired
  protected UserRepository userRepository;
  @Autowired
  protected UserStatusRepository userStatusRepository;
  @Autowired
  protected ChannelRepository channelRepository;
  @Autowired
  protected ReadStatusRepository readStatusRepository;
  @Autowired
  protected MessageRepository messageRepository;
  @Autowired
  protected JdbcTemplate jdbcTemplate;
  @Autowired
  protected EntityManager entityManager;

  protected MockMultipartFile jsonPart(String name, Object value) throws Exception {
    return new MockMultipartFile(
        name,
        "",
        "application/json",
        objectMapper.writeValueAsBytes(value)
    );
  }

  protected MockMultipartHttpServletRequestBuilder multipartPatch(String urlTemplate,
                                                                  Object... uriVars) {
    MockMultipartHttpServletRequestBuilder builder = multipart(urlTemplate, uriVars);
    builder.with(request -> {
      request.setMethod("PATCH");
      return request;
    });
    return builder;
  }

  protected void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  protected User persistUser(String username, String email) {
    User user = userRepository.saveAndFlush(new User(username, email, "password123", null));
    userStatusRepository.saveAndFlush(new UserStatus(user, Instant.now()));
    flushAndClear();
    return userRepository.findById(user.getId()).orElseThrow();
  }

  protected Channel persistPublicChannel(String name, String description) {
    Channel channel = channelRepository.saveAndFlush(new Channel(ChannelType.PUBLIC, name, description));
    flushAndClear();
    return channelRepository.findById(channel.getId()).orElseThrow();
  }

  protected Channel persistPrivateChannel() {
    Channel channel = channelRepository.saveAndFlush(new Channel(ChannelType.PRIVATE, null, null));
    flushAndClear();
    return channelRepository.findById(channel.getId()).orElseThrow();
  }

  protected ReadStatus persistReadStatus(User user, Channel channel) {
    User managedUser = entityManager.getReference(User.class, user.getId());
    Channel managedChannel = entityManager.getReference(Channel.class, channel.getId());
    ReadStatus readStatus = readStatusRepository.saveAndFlush(
        new ReadStatus(managedUser, managedChannel, Instant.now()));
    flushAndClear();
    return readStatusRepository.findById(readStatus.getId()).orElseThrow();
  }

  protected Message persistMessage(Channel channel, User author, String content, Instant createdAt) {
    Channel managedChannel = entityManager.getReference(Channel.class, channel.getId());
    User managedAuthor = entityManager.getReference(User.class, author.getId());
    Message message = messageRepository.saveAndFlush(
        new Message(content, managedChannel, managedAuthor, List.of()));

    jdbcTemplate.update(
        "update messages set created_at = ? where id = ?",
        Timestamp.from(createdAt),
        message.getId()
    );
    flushAndClear();
    return messageRepository.findById(message.getId()).orElseThrow();
  }
}
