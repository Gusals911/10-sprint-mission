package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.config.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;

@Mapper(componentModel = "spring", uses = {BinaryContentMapper.class})
public abstract class UserMapper {

  @Autowired
  private SessionRegistry sessionRegistry;

  @Mapping(target = "online", expression = "java(resolveOnline(user))")
  public abstract UserDto toDto(User user);

  protected Boolean resolveOnline(User user) {
    // UserStatus 엔티티 대신 SessionRegistry의 활성 세션 존재 여부로 온라인 상태를 계산
    return sessionRegistry.getAllPrincipals().stream()
        .filter(DiscodeitUserDetails.class::isInstance)
        .map(DiscodeitUserDetails.class::cast)
        .filter(userDetails -> user.getId().equals(userDetails.getUserDto().id()))
        .anyMatch(userDetails -> !sessionRegistry.getAllSessions(userDetails, false).isEmpty());
  }
}
