package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import java.util.List;
import java.util.UUID;
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
public class ChannelRepositorySliceTest {

  @Autowired
  private ChannelRepository channelRepository;
  @Autowired
  private TestEntityManager entityManager;

  // public, private 채널 조건 조회 성공
  @Test
  @DisplayName("public 채널과 선택된 private 채널 조회를 검증한다.")
  void find_all_by_type_or_id_in_success() {
    // given
    Channel publicChannel = persistChannel(ChannelType.PUBLIC, "backend", "backend channel");
    Channel anotherPublicChannel = persistChannel(ChannelType.PUBLIC, "frontend", "frontend channel");
    Channel privateChannel = persistChannel(ChannelType.PRIVATE, null, null);

    // when
    List<Channel> result = channelRepository.findAllByTypeOrIdIn(
        ChannelType.PUBLIC,
        List.of(privateChannel.getId())
    );

    // then
    assertThat(result)
        .extracting(Channel::getId)
        .containsExactlyInAnyOrder(
            publicChannel.getId(),
            anotherPublicChannel.getId(),
            privateChannel.getId()
        );
  }

  // public, private 채널 조건 조회 실패
  @Test
  @DisplayName("일치하는 private 채널이 없으면 public 채널만 반환한다.")
  void find_all_by_type_or_id_in_fail() {
    // given
    Channel publicChannel = persistChannel(ChannelType.PUBLIC, "backend", "backend channel");
    Channel privateChannel = persistChannel(ChannelType.PRIVATE, null, null);

    // when
    List<Channel> result = channelRepository.findAllByTypeOrIdIn(
        ChannelType.PUBLIC,
        List.of(UUID.randomUUID())
    );

    // then
    assertThat(result)
        .extracting(Channel::getId)
        .containsExactly(publicChannel.getId());
    assertThat(result)
        .extracting(Channel::getId)
        .doesNotContain(privateChannel.getId());
  }

  private Channel persistChannel(ChannelType type, String name, String description) {
    Channel channel = channelRepository.saveAndFlush(new Channel(type, name, description));
    entityManager.clear();
    return channel;
  }
}
