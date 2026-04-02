package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class BasicChannerServiceTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ReadStatusRepository readStatusRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChannelMapper channelMapper;
    @InjectMocks
    private BasicChannelService basicChannelService;

    // 공개 채널 생성 성공
    @Test
    @DisplayName("공개 채널 생성을 검증한다.")
    void create_public_channel_success() {
        // given
        ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);

        PublicChannelCreateRequest request = new PublicChannelCreateRequest("backend", "backend channel");
        ChannelDto expected = new ChannelDto(UUID.randomUUID(), ChannelType.PUBLIC, "backend",
                "backend channel", null, null);

        given(channelMapper.toDto(any())).willReturn(expected);

        // when
        ChannelDto result = basicChannelService.create(request);

        // then
        assertThat(result).isEqualTo(expected);

        then(channelRepository).should().save(channelCaptor.capture());

        Channel savedChannel = channelCaptor.getValue();

        then(channelMapper).should().toDto(savedChannel);
        then(readStatusRepository).should(never()).saveAll(any());

        assertThat(savedChannel.getType()).isEqualTo(ChannelType.PUBLIC);
        assertThat(savedChannel.getName()).isEqualTo("backend");
        assertThat(savedChannel.getDescription()).isEqualTo("backend channel");
    }

    // 비공개 채널 생성 성공
    @Test
    @DisplayName("비공개 채널 생성을 검증한다.")
    @SuppressWarnings("unchecked")
    void create_private_channel_success() {
        // given
        ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
        ArgumentCaptor<List> readStatusCaptor = ArgumentCaptor.forClass(List.class);

        UUID firstParticipantId = UUID.randomUUID();
        UUID secondParticipantId = UUID.randomUUID();
        PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(
                List.of(firstParticipantId, secondParticipantId));
        User firstUser = new User("gusals", "gusals@naver.com", "1234", null);
        User secondUser = new User("mung", "mung@naver.com", "1234", null);
        ChannelDto expected = new ChannelDto(UUID.randomUUID(), ChannelType.PRIVATE, null, null, null, null);

        given(userRepository.findAllById(request.participantIds())).willReturn(List.of(firstUser, secondUser));
        given(channelMapper.toDto(any())).willReturn(expected);

        // when
        ChannelDto result = basicChannelService.create(request);

        // then
        then(channelRepository).should().save(channelCaptor.capture());
        then(readStatusRepository).should().saveAll(readStatusCaptor.capture());

        Channel savedChannel = channelCaptor.getValue();
        List<ReadStatus> savedReadStatuses = new ArrayList<>();
        readStatusCaptor.getValue().forEach(readStatus -> savedReadStatuses.add((ReadStatus) readStatus));

        assertThat(result).isEqualTo(expected);
        assertThat(savedChannel.getType()).isEqualTo(ChannelType.PRIVATE);
        assertThat(savedChannel.getName()).isNull();
        assertThat(savedChannel.getDescription()).isNull();
        assertThat(savedReadStatuses).hasSize(2);
        assertThat(savedReadStatuses)
                .extracting(ReadStatus::getUser)
                .containsExactly(firstUser, secondUser);
        assertThat(savedReadStatuses)
                .extracting(ReadStatus::getChannel)
                .containsOnly(savedChannel);

        then(channelMapper).should().toDto(savedChannel);
    }
}
