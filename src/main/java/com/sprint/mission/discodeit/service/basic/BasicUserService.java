package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.user.UserAlreadyExistsException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.storage.BinaryContentStorageSupport;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicUserService implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final BinaryContentRepository binaryContentRepository;
  private final BinaryContentStorageSupport binaryContentStorageSupport;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  @Override
  public UserDto create(UserCreateRequest userCreateRequest,
      Optional<BinaryContentCreateRequest> optionalProfileCreateRequest) {
    log.debug("?ъ슜???앹꽦 ?쒖옉: {}", userCreateRequest);

    String username = userCreateRequest.username();
    String email = userCreateRequest.email();

    if (userRepository.existsByEmail(email)) {
      throw UserAlreadyExistsException.withEmail(email);
    }
    if (userRepository.existsByUsername(username)) {
      throw UserAlreadyExistsException.withUsername(username);
    }

    BinaryContent nullableProfile = optionalProfileCreateRequest
        .map(this::saveBinaryContent)
        .orElse(null);
    String password = passwordEncoder.encode(userCreateRequest.password());

    User user = new User(username, email, password, nullableProfile);
    Instant now = Instant.now();
    new UserStatus(user, now);

    userRepository.save(user);
    log.info("?ъ슜???앹꽦 ?꾨즺: id={}, username={}", user.getId(), username);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto find(UUID userId) {
    log.debug("?ъ슜??議고쉶 ?쒖옉: id={}", userId);
    UserDto userDto = userRepository.findById(userId)
        .map(userMapper::toDto)
        .orElseThrow(() -> UserNotFoundException.withId(userId));
    log.info("?ъ슜??議고쉶 ?꾨즺: id={}", userId);
    return userDto;
  }

  @Override
  public List<UserDto> findAll() {
    log.debug("紐⑤뱺 ?ъ슜??議고쉶 ?쒖옉");
    List<UserDto> userDtos = userRepository.findAllWithProfileAndStatus()
        .stream()
        .map(userMapper::toDto)
        .toList();
    log.info("紐⑤뱺 ?ъ슜??議고쉶 ?꾨즺: 珥?{}紐?", userDtos.size());
    return userDtos;
  }

  @Transactional
  @Override
  public UserDto update(UUID userId, UserUpdateRequest userUpdateRequest,
      Optional<BinaryContentCreateRequest> optionalProfileCreateRequest) {
    log.debug("?ъ슜???섏젙 ?쒖옉: id={}, request={}", userId, userUpdateRequest);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    String newUsername = userUpdateRequest.newUsername();
    String newEmail = userUpdateRequest.newEmail();

    if (newEmail != null && userRepository.existsByEmailAndIdNot(newEmail, userId)) {
      throw UserAlreadyExistsException.withEmail(newEmail);
    }

    if (newUsername != null && userRepository.existsByUsernameAndIdNot(newUsername, userId)) {
      throw UserAlreadyExistsException.withUsername(newUsername);
    }

    BinaryContent previousProfile = user.getProfile();
    BinaryContent nullableProfile = optionalProfileCreateRequest
        .map(this::saveBinaryContent)
        .orElse(null);

    if (nullableProfile != null && previousProfile != null) {
      binaryContentStorageSupport.deleteAfterCommit(previousProfile.getId());
    }

    String newPassword = Optional.ofNullable(userUpdateRequest.newPassword())
        .map(passwordEncoder::encode)
        .orElse(null);
    user.update(newUsername, newEmail, newPassword, nullableProfile);

    log.info("?ъ슜???섏젙 ?꾨즺: id={}", userId);
    return userMapper.toDto(user);
  }

  @Transactional
  @Override
  public void delete(UUID userId) {
    log.debug("?ъ슜????젣 ?쒖옉: id={}", userId);

    User user = userRepository.findByIdWithProfile(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    if (user.getProfile() != null) {
      binaryContentStorageSupport.deleteAfterCommit(user.getProfile().getId());
    }

    userRepository.delete(user);
    log.info("?ъ슜????젣 ?꾨즺: id={}", userId);
  }

  private BinaryContent saveBinaryContent(BinaryContentCreateRequest request) {
    BinaryContent binaryContent = new BinaryContent(
        request.fileName(),
        (long) request.bytes().length,
        request.contentType()
    );
    binaryContentRepository.save(binaryContent);
    binaryContentStorageSupport.put(binaryContent.getId(), request.bytes());
    return binaryContent;
  }
}
