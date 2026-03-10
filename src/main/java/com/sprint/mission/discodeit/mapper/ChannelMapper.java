package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ChannelMapper {

    @Autowired
    protected ReadStatusRepository readStatusRepository;

    @Autowired
    protected MessageRepository messageRepository;

    @Autowired
    protected UserMapper userMapper;

    @Mapping(target = "participants", expression = "java(resolveParticipants(channel))")
    @Mapping(target = "lastMessageAt", expression = "java(resolveLastMessageAt(channel))")
    public abstract ChannelDto toDto(Channel channel);

    protected List<UserDto> resolveParticipants(Channel channel) {
        return readStatusRepository.findAllByChannel_Id(channel.getId()).stream()
                .map(ReadStatus::getUser)
                .collect(
                        LinkedHashMap<UUID, UserDto>::new,
                        (map, user) -> map.putIfAbsent(user.getId(), userMapper.toDto(user)),
                        LinkedHashMap::putAll
                )
                .values()
                .stream()
                .toList();
    }

    protected Instant resolveLastMessageAt(Channel channel) {
        return messageRepository.findTopByChannel_IdOrderByCreatedAtDesc(channel.getId())
                .map(Message::getCreatedAt)
                .orElse(null);
    }
}