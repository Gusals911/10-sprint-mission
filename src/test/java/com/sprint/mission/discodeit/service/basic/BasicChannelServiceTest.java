package com.sprint.mission.discodeit.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.channel.InvalidChannelParticipantsException;
import com.sprint.mission.discodeit.exception.channel.PrivateChannelUpdateException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorageSupport;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BasicChannelServiceTest {

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

  @Mock
  private BinaryContentStorageSupport binaryContentStorageSupport;

  @InjectMocks
  private BasicChannelService channelService;

  private UUID channelId;
  private UUID userId;
  private UUID secondUserId;
  private String channelName;
  private String channelDescription;
  private Channel channel;
  private ChannelDto channelDto;
  private User user;
  private User secondUser;

  @BeforeEach
  void setUp() {
    channelId = UUID.randomUUID();
    userId = UUID.randomUUID();
    secondUserId = UUID.randomUUID();
    channelName = "testChannel";
    channelDescription = "testDescription";

    channel = new Channel(ChannelType.PUBLIC, channelName, channelDescription);
    ReflectionTestUtils.setField(channel, "id", channelId);
    channelDto = new ChannelDto(channelId, ChannelType.PUBLIC, channelName, channelDescription,
        List.of(), Instant.now());
    user = new User("testUser", "test@example.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);
    secondUser = new User("secondUser", "second@example.com", "password", null);
    ReflectionTestUtils.setField(secondUser, "id", secondUserId);
  }

  @Test
  @DisplayName("怨듦컻 梨꾨꼸 ?앹꽦 ?깃났")
  void createPublicChannel_Success() {
    PublicChannelCreateRequest request = new PublicChannelCreateRequest(channelName,
        channelDescription);
    given(channelMapper.toDto(any(Channel.class))).willReturn(channelDto);

    ChannelDto result = channelService.create(request);

    assertThat(result).isEqualTo(channelDto);
    verify(channelRepository).save(any(Channel.class));
  }

  @Test
  @DisplayName("鍮꾧났媛?梨꾨꼸 ?앹꽦 ?깃났")
  void createPrivateChannel_Success() {
    List<UUID> participantIds = List.of(userId, secondUserId);
    PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(participantIds);
    given(userRepository.findAllById(eq(participantIds))).willReturn(List.of(user, secondUser));
    given(channelMapper.toDto(any(Channel.class))).willReturn(channelDto);

    ChannelDto result = channelService.create(request);

    assertThat(result).isEqualTo(channelDto);
    verify(channelRepository).save(any(Channel.class));
    verify(readStatusRepository).<ReadStatus>saveAll(anyList());
  }

  @Test
  @DisplayName("鍮꾧났媛?梨꾨꼸 ?앹꽦 ???듯븳 以묐났 李몄뿬?먮뒗 ?ㅼ슂泥섎줈 諛붾뇦")
  void createPrivateChannel_WithDuplicateParticipants_ThrowsException() {
    PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(List.of(userId, userId));

    assertThatThrownBy(() -> channelService.create(request))
        .isInstanceOf(InvalidChannelParticipantsException.class);
  }

  @Test
  @DisplayName("鍮꾧났媛?梨꾨꼸 ?앹꽦 ??議댁옱?섏? ?딅뒗 李몄뿬?먭? ?덉쑝硫?ㅽ뙣")
  void createPrivateChannel_WithMissingParticipant_ThrowsException() {
    UUID missingUserId = UUID.randomUUID();
    List<UUID> participantIds = List.of(userId, missingUserId);
    PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(participantIds);
    given(userRepository.findAllById(eq(participantIds))).willReturn(List.of(user));

    assertThatThrownBy(() -> channelService.create(request))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  @DisplayName("梨꾨꼸 議고쉶 ?깃났")
  void findChannel_Success() {
    given(channelRepository.findById(eq(channelId))).willReturn(Optional.of(channel));
    given(channelMapper.toDto(any(Channel.class))).willReturn(channelDto);

    ChannelDto result = channelService.find(channelId);

    assertThat(result).isEqualTo(channelDto);
  }

  @Test
  @DisplayName("議댁옱?섏? ?딅뒗 梨꾨꼸 議고쉶 ???ㅽ뙣")
  void findChannel_WithNonExistentId_ThrowsException() {
    given(channelRepository.findById(eq(channelId))).willReturn(Optional.empty());

    assertThatThrownBy(() -> channelService.find(channelId))
        .isInstanceOf(ChannelNotFoundException.class);
  }

  @Test
  @DisplayName("?ъ슜?먮퀎 梨꾨꼸 紐⑸줉 議고쉶 ?깃났")
  void findAllByUserId_Success() {
    List<ReadStatus> readStatuses = List.of(new ReadStatus(user, channel, Instant.now()));
    given(readStatusRepository.findAllByUserId(eq(userId))).willReturn(readStatuses);
    given(channelRepository.findAllByTypeOrIdIn(eq(ChannelType.PUBLIC), eq(List.of(channel.getId()))))
        .willReturn(List.of(channel));
    given(channelMapper.toDto(any(Channel.class))).willReturn(channelDto);

    List<ChannelDto> result = channelService.findAllByUserId(userId);

    assertThat(result).containsExactly(channelDto);
  }

  @Test
  @DisplayName("怨듦컻 梨꾨꼸 ?섏젙 ?깃났")
  void updatePublicChannel_Success() {
    String newName = "newChannelName";
    String newDescription = "newDescription";
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest(newName, newDescription);

    given(channelRepository.findById(eq(channelId))).willReturn(Optional.of(channel));
    given(channelMapper.toDto(any(Channel.class))).willReturn(channelDto);

    ChannelDto result = channelService.update(channelId, request);

    assertThat(result).isEqualTo(channelDto);
  }

  @Test
  @DisplayName("鍮꾧났媛?梨꾨꼸 ?섏젙 ?쒕룄 ???ㅽ뙣")
  void updatePrivateChannel_ThrowsException() {
    Channel privateChannel = new Channel(ChannelType.PRIVATE, null, null);
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("newName",
        "newDescription");
    given(channelRepository.findById(eq(channelId))).willReturn(Optional.of(privateChannel));

    assertThatThrownBy(() -> channelService.update(channelId, request))
        .isInstanceOf(PrivateChannelUpdateException.class);
  }

  @Test
  @DisplayName("議댁옱?섏? ?딅뒗 梨꾨꼸 ?섏젙 ?쒕룄 ???ㅽ뙣")
  void updateChannel_WithNonExistentId_ThrowsException() {
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("newName",
        "newDescription");
    given(channelRepository.findById(eq(channelId))).willReturn(Optional.empty());

    assertThatThrownBy(() -> channelService.update(channelId, request))
        .isInstanceOf(ChannelNotFoundException.class);
  }

  @Test
  @DisplayName("梨꾨꼸 ??젣 ?깃났")
  void deleteChannel_Success() {
    BinaryContent attachment = new BinaryContent("test.txt", 100L, "text/plain");
    UUID attachmentId = UUID.randomUUID();
    ReflectionTestUtils.setField(attachment, "id", attachmentId);
    Message message = new Message("content", channel, user, List.of(attachment));

    given(channelRepository.existsById(eq(channelId))).willReturn(true);
    given(messageRepository.findAllByChannelIdWithAttachments(eq(channelId))).willReturn(
        List.of(message));

    channelService.delete(channelId);

    verify(binaryContentStorageSupport).deleteAfterCommit(attachmentId);
    verify(messageRepository).deleteAll(anyList());
    verify(readStatusRepository).deleteAllByChannelId(eq(channelId));
    verify(channelRepository).deleteById(eq(channelId));
  }

  @Test
  @DisplayName("議댁옱?섏? ?딅뒗 梨꾨꼸 ??젣 ?쒕룄 ???ㅽ뙣")
  void deleteChannel_WithNonExistentId_ThrowsException() {
    given(channelRepository.existsById(eq(channelId))).willReturn(false);

    assertThatThrownBy(() -> channelService.delete(channelId))
        .isInstanceOf(ChannelNotFoundException.class);
  }
}
