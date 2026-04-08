package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
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

    // 유저별 채널 조회 성공
    @Test
    @DisplayName("유저가 참여한 채널 목록 조회를 검증한다.")
    void find_all_by_user_id_success() {
        // given
        UUID userId = UUID.randomUUID();
        User user = new User("gusals", "gusals@naver.com", "1234", null);
        Channel privateChannel = new Channel(ChannelType.PRIVATE, null, null);
        Channel publicChannel = new Channel(ChannelType.PUBLIC, "backend", "backend channel");
        ReadStatus readStatus = new ReadStatus(user, privateChannel, privateChannel.getCreatedAt());
        ChannelDto firstExpected = new ChannelDto(UUID.randomUUID(), ChannelType.PUBLIC, "backend",
                "backend channel", null, null);
        ChannelDto secondExpected = new ChannelDto(UUID.randomUUID(), ChannelType.PRIVATE, null, null, null, null);

        given(readStatusRepository.findAllByUserId(userId)).willReturn(List.of(readStatus));
        given(channelRepository.findAllByTypeOrIdIn(eq(ChannelType.PUBLIC), anyList()))
                .willReturn(List.of(publicChannel, privateChannel));
        given(channelMapper.toDto(publicChannel)).willReturn(firstExpected);
        given(channelMapper.toDto(privateChannel)).willReturn(secondExpected);

        // when
        List<ChannelDto> result = basicChannelService.findAllByUserId(userId);

        // then
        assertThat(result).containsExactly(firstExpected, secondExpected);

        then(readStatusRepository).should().findAllByUserId(userId);
        then(channelRepository).should().findAllByTypeOrIdIn(eq(ChannelType.PUBLIC), anyList());
        then(channelMapper).should().toDto(publicChannel);
        then(channelMapper).should().toDto(privateChannel);
    }

    // 공개 채널 수정 성공
    @Test
    @DisplayName("공개 채널 수정을 검증한다.")
    void update_public_channel_success() {
        // given
        UUID channelId = UUID.randomUUID();
        PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("frontend", "frontend channel");
        Channel channel = new Channel(ChannelType.PUBLIC, "backend", "backend channel");
        ChannelDto expected = new ChannelDto(channelId, ChannelType.PUBLIC, "frontend",
                "frontend channel", null, null);

        given(channelRepository.findById(channelId)).willReturn(java.util.Optional.of(channel));
        given(channelMapper.toDto(any())).willReturn(expected);

        // when
        ChannelDto result = basicChannelService.update(channelId, request);

        // then
        assertThat(result).isEqualTo(expected);
        assertThat(channel.getName()).isEqualTo("frontend");
        assertThat(channel.getDescription()).isEqualTo("frontend channel");

        then(channelRepository).should().findById(channelId);
        then(channelMapper).should().toDto(channel);
    }

    // 채널 삭제 성공
    @Test
    @DisplayName("채널 삭제를 검증한다.")
    void delete_success() {
        // given
        UUID channelId = UUID.randomUUID();

        given(channelRepository.existsById(channelId)).willReturn(true);

        // when
        Throwable result = catchThrowable(() -> basicChannelService.delete(channelId));

        // then
        assertThat(result).doesNotThrowAnyException();

        then(channelRepository).should().existsById(channelId);
        then(messageRepository).should().deleteAllByChannelId(channelId);
        then(readStatusRepository).should().deleteAllByChannelId(channelId);
        then(channelRepository).should().deleteById(channelId);
    }
}
