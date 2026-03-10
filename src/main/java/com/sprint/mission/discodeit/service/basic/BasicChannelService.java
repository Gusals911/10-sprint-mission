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
import com.sprint.mission.discodeit.service.ChannelService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class BasicChannelService implements ChannelService {

  private final ChannelRepository channelRepository;
  private final ReadStatusRepository readStatusRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ChannelMapper channelMapper;

  @Override
  @Transactional
  public ChannelDto create(PublicChannelCreateRequest request) {
    Channel saved = channelRepository.save(
            new Channel(ChannelType.PUBLIC, request.name(), request.description())
    );
    return channelMapper.toDto(saved);
  }

  @Override
  @Transactional
  public ChannelDto create(PrivateChannelCreateRequest request) {
    Channel channel = channelRepository.save(new Channel(ChannelType.PRIVATE, null, null));

    List<User> participants = request.participantIds().stream()
            .map(userId -> userRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found")))
            .toList();

    participants.stream()
            .map(user -> new ReadStatus(user, channel, channel.getCreatedAt()))
            .forEach(readStatusRepository::save);

    return channelMapper.toDto(channel);
  }

  @Override
  public ChannelDto find(UUID channelId) {
    return channelRepository.findById(channelId)
            .map(channelMapper::toDto)
            .orElseThrow(() -> new NoSuchElementException("Channel with id " + channelId + " not found"));
  }

  @Override
  public List<ChannelDto> findAllByUserId(UUID userId) {
    List<UUID> mySubscribedChannelIds = readStatusRepository.findAllByUser_Id(userId).stream()
            .map(ReadStatus::getChannelId)
            .toList();

    return channelRepository.findAll().stream()
            .filter(channel -> channel.getType() == ChannelType.PUBLIC || mySubscribedChannelIds.contains(channel.getId()))
            .map(channelMapper::toDto)
            .toList();
  }

  @Override
  @Transactional
  public ChannelDto update(UUID channelId, PublicChannelUpdateRequest request) {
    Channel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> new NoSuchElementException("Channel with id " + channelId + " not found"));

    if (channel.getType() == ChannelType.PRIVATE) {
      throw new IllegalArgumentException("Private channel cannot be updated");
    }

    channel.update(request.newName(), request.newDescription());
    return channelMapper.toDto(channel);
  }

  @Override
  @Transactional
  public void delete(UUID channelId) {
    if (!channelRepository.existsById(channelId)) {
      throw new NoSuchElementException("Channel with id " + channelId + " not found");
    }
    messageRepository.deleteAllByChannel_Id(channelId);
    readStatusRepository.deleteAllByChannel_Id(channelId);
    channelRepository.deleteById(channelId);
  }
}