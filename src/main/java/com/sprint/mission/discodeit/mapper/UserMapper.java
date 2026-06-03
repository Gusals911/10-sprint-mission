package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.config.security.JwtRegistry;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {BinaryContentMapper.class})
public abstract class UserMapper {

  @Autowired
  private JwtRegistry jwtRegistry;

  @Mapping(target = "online", expression = "java(resolveOnline(user))")
  public abstract UserDto toDto(User user);

  protected Boolean resolveOnline(User user) {
    // JWT 레지스트리에 활성 토큰이 남아 있으면 온라인 상태로 판단
    return jwtRegistry.hasActiveJwtInformationByUserId(user.getId());
  }
}
