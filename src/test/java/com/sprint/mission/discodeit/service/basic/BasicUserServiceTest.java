package com.sprint.mission.discodeit.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.user.UserAlreadyExistsException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorageSupport;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BasicUserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private BinaryContentRepository binaryContentRepository;

  @Mock
  private BinaryContentStorageSupport binaryContentStorageSupport;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private BasicUserService userService;

  private UUID userId;
  private String username;
  private String email;
  private String password;
  private User user;
  private UserDto userDto;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    username = "testUser";
    email = "test@example.com";
    password = "password123";

    user = new User(username, email, password, null);
    ReflectionTestUtils.setField(user, "id", userId);
    userDto = new UserDto(userId, username, email, null, true);
  }

  @Test
  @DisplayName("?ъ슜???앹꽦 ?깃났")
  void createUser_Success() {
    UserCreateRequest request = new UserCreateRequest(username, email, password);
    given(userRepository.existsByEmail(eq(email))).willReturn(false);
    given(userRepository.existsByUsername(eq(username))).willReturn(false);
    given(passwordEncoder.encode(eq(password))).willReturn("encoded-password");
    given(userMapper.toDto(any(User.class))).willReturn(userDto);

    UserDto result = userService.create(request, Optional.empty());

    assertThat(result).isEqualTo(userDto);
    verify(passwordEncoder).encode(password);
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("?대? 議댁옱?섎뒗 ?대찓?쇰줈 ?ъ슜???앹꽦 ?쒕룄 ???ㅽ뙣")
  void createUser_WithExistingEmail_ThrowsException() {
    UserCreateRequest request = new UserCreateRequest(username, email, password);
    given(userRepository.existsByEmail(eq(email))).willReturn(true);

    assertThatThrownBy(() -> userService.create(request, Optional.empty()))
        .isInstanceOf(UserAlreadyExistsException.class);
  }

  @Test
  @DisplayName("?대? 議댁옱?섎뒗 ?ъ슜?먮챸?쇰줈 ?ъ슜???앹꽦 ?쒕룄 ???ㅽ뙣")
  void createUser_WithExistingUsername_ThrowsException() {
    UserCreateRequest request = new UserCreateRequest(username, email, password);
    given(userRepository.existsByEmail(eq(email))).willReturn(false);
    given(userRepository.existsByUsername(eq(username))).willReturn(true);

    assertThatThrownBy(() -> userService.create(request, Optional.empty()))
        .isInstanceOf(UserAlreadyExistsException.class);
  }

  @Test
  @DisplayName("?ъ슜??議고쉶 ?깃났")
  void findUser_Success() {
    given(userRepository.findById(eq(userId))).willReturn(Optional.of(user));
    given(userMapper.toDto(any(User.class))).willReturn(userDto);

    UserDto result = userService.find(userId);

    assertThat(result).isEqualTo(userDto);
  }

  @Test
  @DisplayName("議댁옱?섏? ?딅뒗 ?ъ슜??議고쉶 ???ㅽ뙣")
  void findUser_WithNonExistentId_ThrowsException() {
    given(userRepository.findById(eq(userId))).willReturn(Optional.empty());

    assertThatThrownBy(() -> userService.find(userId))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  @DisplayName("?ъ슜???섏젙 ?깃났")
  void updateUser_Success() {
    String newUsername = "newUsername";
    String newEmail = "new@example.com";
    String newPassword = "newPassword";
    UserUpdateRequest request = new UserUpdateRequest(newUsername, newEmail, newPassword);

    given(userRepository.findById(eq(userId))).willReturn(Optional.of(user));
    given(userRepository.existsByEmailAndIdNot(eq(newEmail), eq(userId))).willReturn(false);
    given(userRepository.existsByUsernameAndIdNot(eq(newUsername), eq(userId))).willReturn(false);
    given(passwordEncoder.encode(eq(newPassword))).willReturn("encoded-new-password");
    given(userMapper.toDto(any(User.class))).willReturn(userDto);

    UserDto result = userService.update(userId, request, Optional.empty());

    assertThat(result).isEqualTo(userDto);
    verify(passwordEncoder).encode(newPassword);
  }

  @Test
  @DisplayName("媛숈? email怨?username ?섏젙??_?곗옄??以묐났?????섎떎")
  void updateUser_WithSameEmailAndUsername_Success() {
    UserUpdateRequest request = new UserUpdateRequest(username, email, null);

    given(userRepository.findById(eq(userId))).willReturn(Optional.of(user));
    given(userRepository.existsByEmailAndIdNot(eq(email), eq(userId))).willReturn(false);
    given(userRepository.existsByUsernameAndIdNot(eq(username), eq(userId))).willReturn(false);
    given(userMapper.toDto(any(User.class))).willReturn(userDto);

    UserDto result = userService.update(userId, request, Optional.empty());

    assertThat(result).isEqualTo(userDto);
  }

  @Test
  @DisplayName("議댁옱?섏? ?딅뒗 ?ъ슜???섏젙 ?쒕룄 ???ㅽ뙣")
  void updateUser_WithNonExistentId_ThrowsException() {
    UserUpdateRequest request = new UserUpdateRequest("newUsername", "new@example.com",
        "newPassword");
    given(userRepository.findById(eq(userId))).willReturn(Optional.empty());

    assertThatThrownBy(() -> userService.update(userId, request, Optional.empty()))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  @DisplayName("?ъ슜????젣 ?깃났")
  void deleteUser_Success() {
    given(userRepository.findByIdWithProfile(eq(userId))).willReturn(Optional.of(user));

    userService.delete(userId);

    verify(userRepository).delete(eq(user));
  }

  @Test
  @DisplayName("議댁옱?섏? ?딅뒗 ?ъ슜????젣 ?쒕룄 ???ㅽ뙣")
  void deleteUser_WithNonExistentId_ThrowsException() {
    given(userRepository.findByIdWithProfile(eq(userId))).willReturn(Optional.empty());

    assertThatThrownBy(() -> userService.delete(userId))
        .isInstanceOf(UserNotFoundException.class);
  }
}
