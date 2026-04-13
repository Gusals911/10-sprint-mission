package com.sprint.mission.discodeit.service.basic;

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
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.storage.BinaryContentStorageSupport;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicChannelService implements ChannelService {

  private final ChannelRepository channelRepository;
  private final ReadStatusRepository readStatusRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ChannelMapper channelMapper;
  private final BinaryContentStorageSupport binaryContentStorageSupport;

  @Transactional
  @Override
  public ChannelDto create(PublicChannelCreateRequest request) {
    log.debug("채널 생성 시작: {}", request);
    String name = request.name();
    String description = request.description();
    Channel channel = new Channel(ChannelType.PUBLIC, name, description);

    channelRepository.save(channel);
    log.info("채널 생성 완료: id={}, name={}", channel.getId(), channel.getName());
    return channelMapper.toDto(channel);
  }

  @Transactional
  @Override
  public ChannelDto create(PrivateChannelCreateRequest request) {
    log.debug("채널 생성 시작: {}", request);

    List<UUID> participantIds = request.participantIds().stream()
        .distinct()
        .toList();
    if (participantIds.size() < 2) {
      throw InvalidChannelParticipantsException.withParticipantIds(request.participantIds());
    }

    List<User> participants = userRepository.findAllById(participantIds);
    if (participants.size() != participantIds.size()) {
      UUID missingParticipantId = participantIds.stream()
          .filter(participantId -> participants.stream()
              .noneMatch(user -> user.getId().equals(participantId)))
          .findFirst()
          .orElseThrow(() -> InvalidChannelParticipantsException.withParticipantIds(
              request.participantIds()));
      throw UserNotFoundException.withId(missingParticipantId);
    }

    Channel channel = new Channel(ChannelType.PRIVATE, null, null);
    channelRepository.save(channel);

    List<ReadStatus> readStatuses = participants.stream()
        .map(user -> new ReadStatus(user, channel, channel.getCreatedAt()))
        .toList();
    readStatusRepository.saveAll(readStatuses);

    log.info("채널 생성 완료: id={}, name={}", channel.getId(), channel.getName());
    return channelMapper.toDto(channel);
  }

  @Transactional(readOnly = true)
  @Override
  public ChannelDto find(UUID channelId) {
    return channelRepository.findById(channelId)
        .map(channelMapper::toDto)
        .orElseThrow(() -> ChannelNotFoundException.withId(channelId));
  }

  @Transactional(readOnly = true)
  @Override
  public List<ChannelDto> findAllByUserId(UUID userId) {
    List<UUID> mySubscribedChannelIds = readStatusRepository.findAllByUserId(userId).stream()
        .map(ReadStatus::getChannel)
        .map(Channel::getId)
        .toList();

    return channelRepository.findAllByTypeOrIdIn(ChannelType.PUBLIC, mySubscribedChannelIds)
        .stream()
        .map(channelMapper::toDto)
        .toList();
  }

  @Transactional
  @Override
  public ChannelDto update(UUID channelId, PublicChannelUpdateRequest request) {
    log.debug("채널 수정 시작: id={}, request={}", channelId, request);
    String newName = request.newName();
    String newDescription = request.newDescription();
    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(() -> ChannelNotFoundException.withId(channelId));
    if (channel.getType().equals(ChannelType.PRIVATE)) {
      throw PrivateChannelUpdateException.forChannel(channelId);
    }
    channel.update(newName, newDescription);
    log.info("채널 수정 완료: id={}, name={}", channelId, channel.getName());
    return channelMapper.toDto(channel);
  }

  @Transactional
  @Override
  public void delete(UUID channelId) {
    log.debug("채널 삭제 시작: id={}", channelId);
    if (!channelRepository.existsById(channelId)) {
      throw ChannelNotFoundException.withId(channelId);
    }

    List<Message> messages = messageRepository.findAllByChannelIdWithAttachments(channelId);
    if (messages == null) {
      messages = Collections.emptyList();
    }

    List<UUID> attachmentIds = messages.stream()
        .map(Message::getAttachments)
        .flatMap(List::stream)
        .map(BinaryContent::getId)
        .toList();
    if (!attachmentIds.isEmpty()) {
      attachmentIds.forEach(binaryContentStorageSupport::deleteAfterCommit);
    }

    if (messages.isEmpty()) {
      messageRepository.deleteAllByChannelId(channelId);
    } else {
      messageRepository.deleteAll(messages);
    }
    readStatusRepository.deleteAllByChannelId(channelId);

    channelRepository.deleteById(channelId);
    log.info("채널 삭제 완료: id={}", channelId);
  }
}
