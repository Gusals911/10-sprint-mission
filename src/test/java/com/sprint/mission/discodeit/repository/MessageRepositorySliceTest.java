package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MessageRepositorySliceTest {

  @Autowired
  private MessageRepository messageRepository;
  @Autowired
  private ChannelRepository channelRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private UserStatusRepository userStatusRepository;
  @Autowired
  private TestEntityManager entityManager;
  @Autowired
  private JdbcTemplate jdbcTemplate;

  // 채널별 메시지 슬라이스 조회 성공
  @Test
  @DisplayName("cursor 이전 메시지 슬라이스 조회를 검증한다.")
  void find_all_by_channel_id_with_author_success() {
    // given
    Channel channel = persistChannel();
    User author = persistUser("author", "author@example.com");
    Message oldest = persistMessage(channel, author, "oldest",
        Instant.parse("2026-04-05T09:57:00Z"));
    Message middle = persistMessage(channel, author, "middle",
        Instant.parse("2026-04-05T09:58:00Z"));
    Message latest = persistMessage(channel, author, "latest",
        Instant.parse("2026-04-05T09:59:00Z"));
    persistMessage(channel, author, "ignored",
        Instant.parse("2026-04-05T10:05:00Z"));

    // when
    Slice<Message> result = messageRepository.findAllByChannelIdWithAuthor(
        channel.getId(),
        Instant.parse("2026-04-05T10:00:00Z"),
        PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"))
    );

    // then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .extracting(Message::getId)
        .containsExactly(latest.getId(), middle.getId());
    assertThat(result.hasNext()).isTrue();
    assertThat(result.getContent().get(0).getAuthor().getEmail()).isEqualTo("author@example.com");
    assertThat(result.getContent().get(0).getAuthor().getStatus()).isNotNull();
    assertThat(result.getContent().get(1).getId()).isNotEqualTo(oldest.getId());
  }

  // 채널별 메시지 슬라이스 조회 실패
  @Test
  @DisplayName("조건에 맞는 메시지가 없으면 빈 슬라이스를 반환한다.")
  void find_all_by_channel_id_with_author_fail() {
    // given
    Channel channel = persistChannel();
    User author = persistUser("author", "author@example.com");
    persistMessage(channel, author, "message",
        Instant.parse("2026-04-05T10:10:00Z"));

    // when
    Slice<Message> result = messageRepository.findAllByChannelIdWithAuthor(
        channel.getId(),
        Instant.parse("2026-04-05T10:00:00Z"),
        PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"))
    );

    // then
    assertThat(result.getContent()).isEmpty();
    assertThat(result.hasNext()).isFalse();
  }

  // 채널의 마지막 메시지 시간 조회 성공
  @Test
  @DisplayName("채널의 마지막 메시지 시간 조회를 검증한다.")
  void find_last_message_at_by_channel_id_success() {
    // given
    Channel channel = persistChannel();
    User author = persistUser("author", "author@example.com");
    persistMessage(channel, author, "first",
        Instant.parse("2026-04-05T09:57:00Z"));
    persistMessage(channel, author, "second",
        Instant.parse("2026-04-05T10:03:00Z"));

    // when
    Optional<Instant> result = messageRepository.findLastMessageAtByChannelId(channel.getId());

    // then
    assertThat(result).contains(Instant.parse("2026-04-05T10:03:00Z"));
  }

  // 채널의 마지막 메시지 시간 조회 실패
  @Test
  @DisplayName("채널에 메시지가 없으면 빈 값을 반환한다.")
  void find_last_message_at_by_channel_id_fail() {
    // given
    Channel channel = persistChannel();

    // when
    Optional<Instant> result = messageRepository.findLastMessageAtByChannelId(channel.getId());

    // then
    assertThat(result).isEmpty();
  }

  private Channel persistChannel() {
    Channel channel = channelRepository.saveAndFlush(
        new Channel(ChannelType.PUBLIC, "backend", "backend channel"));
    entityManager.clear();
    return channel;
  }

  private User persistUser(String username, String email) {
    User user = userRepository.save(new User(username, email, "password123", null));
    userStatusRepository.save(new UserStatus(user, Instant.parse("2026-04-05T00:00:00Z")));
    entityManager.flush();
    entityManager.clear();
    return user;
  }

  private Message persistMessage(Channel channel, User author, String content, Instant createdAt) {
    Channel managedChannel = entityManager.getEntityManager().getReference(Channel.class, channel.getId());
    User managedAuthor = entityManager.getEntityManager().getReference(User.class, author.getId());
    Message message = messageRepository.saveAndFlush(
        new Message(content, managedChannel, managedAuthor, List.of()));
    jdbcTemplate.update(
        "update messages set created_at = ? where id = ?",
        Timestamp.from(createdAt),
        message.getId()
    );
    entityManager.clear();
    return message;
  }
}
