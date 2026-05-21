package com.sprint.mission.discodeit.config.security;

import com.sprint.mission.discodeit.dto.data.UserDto;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
public class DiscodeitUserDetails implements UserDetails {
  // 인증 Principal에 담아둘 사용자 응답 정보
  private final UserDto userDto;
  // Spring Security가 PasswordEncoder로 검증할 암호화된 비밀번호
  private final String password;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // 사용자 권한을 Spring Security 권한 형식으로 변환
    return List.of(new SimpleGrantedAuthority("ROLE_" + userDto.role().name()));
  }

  @Override
  public String getUsername() {
    return userDto.username();
  }

  @Override
  public String getPassword() {
    return password;
  }

  public UserDto toAuthenticatedUserDto() {
    // 현재 인증된 세션의 사용자 응답에서는 로그인 상태를 true로 반환
    return new UserDto(
        userDto.id(),
        userDto.username(),
        userDto.email(),
        userDto.profile(),
        true,
        userDto.role()
    );
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (!(object instanceof DiscodeitUserDetails that)) {
      return false;
    }

    // SessionRegistry가 동일 사용자의 세션을 정확히 찾을 수 있도록 사용자 ID 기준으로 비교
    return Objects.equals(userDto.id(), that.userDto.id());
  }

  @Override
  public int hashCode() {
    return Objects.hash(userDto.id());
  }
}
