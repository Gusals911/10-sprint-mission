package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.user.DuplicatedEmailException;
import com.sprint.mission.discodeit.exception.user.DuplicatedUsernameException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class BasicUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserStatusRepository userStatusRepository;
    @Mock
    private BinaryContentRepository binaryContentRepository;
    @Mock
    private BinaryContentStorage binaryContentStorage;
    @InjectMocks
    private BasicUserService basicUserService;

    // 프로필 없이 생성 성공
    @Test
    @DisplayName("프로필 없이 기본 유저 생성만 검증한다.")
    void create_no_profile_success() {
        // given
        UserCreateRequest request = new UserCreateRequest("gusals", "gusals@naver.com", "1234");
        Optional<BinaryContentCreateRequest> nullProfile = Optional.empty();
        UserDto expected = new UserDto(UUID.randomUUID(), "gusals", "gusals@naver.com", null, false );

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByUsername(request.username())).willReturn(false);
        given(userMapper.toDto(any())).willReturn(expected);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<UserStatus> userStatusCaptor = ArgumentCaptor.forClass(UserStatus.class);


        // when
        UserDto result = basicUserService.create(request, nullProfile);

        // then
        assertThat(result).isEqualTo(expected);

        then(userRepository).should().existsByEmail(request.email());
        then(userRepository).should().existsByUsername(request.username());
        then(userStatusRepository).should().save(userStatusCaptor.capture());
        then(userRepository).should().save(userCaptor.capture());


        User savedUser = userCaptor.getValue();
        UserStatus savedUserStatus = userStatusCaptor.getValue();

        then(userMapper).should().toDto(savedUser);
        then(binaryContentRepository).should(never()).save(any());
        then(binaryContentStorage).should(never()).put(any(), any());

        assertThat(savedUser.getProfile()).isNull();

        assertThat(savedUser.getUsername()).isEqualTo("gusals");
        assertThat(savedUser.getEmail()).isEqualTo("gusals@naver.com");
        assertThat(savedUserStatus.getUser()).isEqualTo(savedUser);
    }

    // 유저/이메일 중복 실패
    @Test
    @DisplayName("이메일이나 유저명이 중복되면 유저 생성에 실패한다.")
    void create_duplicate_email_or_username_fail() {
        // given
        UserCreateRequest duplicatedEmailRequest = new UserCreateRequest("gusals", "gusals@naver.com", "1234");
        UserCreateRequest duplicatedUsernameRequest = new UserCreateRequest("mung", "mung@naver.com", "1234");
        Optional<BinaryContentCreateRequest> nullProfile = Optional.empty();

        given(userRepository.existsByEmail(duplicatedEmailRequest.email())).willReturn(true);
        given(userRepository.existsByEmail(duplicatedUsernameRequest.email())).willReturn(false);
        given(userRepository.existsByUsername(duplicatedUsernameRequest.username())).willReturn(true);

        // when
        Throwable duplicatedEmailException = catchThrowable(
                () -> basicUserService.create(duplicatedEmailRequest, nullProfile));
        Throwable duplicatedUsernameException = catchThrowable(
                () -> basicUserService.create(duplicatedUsernameRequest, nullProfile));

        // then
        assertThat(duplicatedEmailException).isInstanceOf(DuplicatedEmailException.class);
        assertThat(duplicatedUsernameException).isInstanceOf(DuplicatedUsernameException.class);

        then(userRepository).should().existsByEmail(duplicatedEmailRequest.email());
        then(userRepository).should().existsByEmail(duplicatedUsernameRequest.email());
        then(userRepository).should().existsByUsername(duplicatedUsernameRequest.username());
        then(userRepository).should(never()).save(any());
        then(userStatusRepository).should(never()).save(any());
        then(userMapper).should(never()).toDto(any());
        then(binaryContentRepository).should(never()).save(any());
        then(binaryContentStorage).should(never()).put(any(), any());
    }

    // 프로필 포함 생성 성공
    @Test
    @DisplayName("프로필을 포함한 유저 생성을 검증한다.")
    void create_with_profile_success() {
        //given
        ArgumentCaptor<BinaryContent> binaryContentCaptor = ArgumentCaptor.forClass(BinaryContent.class);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        UserCreateRequest request = new UserCreateRequest("gusals", "gusals@naver.com", "1234");
        BinaryContentCreateRequest tempProfile = new BinaryContentCreateRequest("profile", "image", "test".getBytes());
        Optional<BinaryContentCreateRequest> profile = Optional.of(tempProfile);
        UserDto expected = new UserDto(UUID.randomUUID(), "gusals", "gusals@naver.com", null, false );

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByUsername(request.username())).willReturn(false);
        given(userMapper.toDto(any())).willReturn(expected);



        // when
        UserDto result = basicUserService.create(request, profile);

        // then
        then(userRepository).should().save(userCaptor.capture());
        then(binaryContentRepository).should().save(binaryContentCaptor.capture());

        User savedUser = userCaptor.getValue();
        BinaryContent savedBinaryContent = binaryContentCaptor.getValue();

        assertThat(result).isEqualTo(expected);
        assertThat(savedUser.getProfile()).isNotNull();
        assertThat(savedUser.getProfile()).isEqualTo(savedBinaryContent);

        then(binaryContentStorage).should().put(savedBinaryContent.getId(), tempProfile.bytes());

    }

    // 프로필 포함 수정 성공
    @Test
    @DisplayName("프로필을 포함한 유저 수정을 검증한다.")
    void update_with_profile_success() {
        // given
        ArgumentCaptor<BinaryContent> binaryContentCaptor = ArgumentCaptor.forClass(BinaryContent.class);

        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("mung", "mung@naver.com", "12345678");
        BinaryContentCreateRequest tempProfile = new BinaryContentCreateRequest("profile", "image", "test".getBytes());
        Optional<BinaryContentCreateRequest> profile = Optional.of(tempProfile);
        User user = new User("gusals", "gusals@naver.com", "1234", null);
        UserDto expected = new UserDto(userId, "mung", "mung@naver.com", null, false );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail(request.newEmail())).willReturn(false);
        given(userRepository.existsByUsername(request.newUsername())).willReturn(false);
        given(userMapper.toDto(any())).willReturn(expected);

        // when
        UserDto result = basicUserService.update(userId, request, profile);

        // then
        then(userRepository).should().findById(userId);
        then(userRepository).should().existsByEmail(request.newEmail());
        then(userRepository).should().existsByUsername(request.newUsername());
        then(binaryContentRepository).should().save(binaryContentCaptor.capture());

        BinaryContent savedBinaryContent = binaryContentCaptor.getValue();

        assertThat(result).isEqualTo(expected);
        assertThat(user.getUsername()).isEqualTo("mung");
        assertThat(user.getEmail()).isEqualTo("mung@naver.com");
        assertThat(user.getPassword()).isEqualTo("12345678");
        assertThat(user.getProfile()).isEqualTo(savedBinaryContent);

        then(userMapper).should().toDto(user);
        then(binaryContentStorage).should().put(savedBinaryContent.getId(), tempProfile.bytes());
    }

    // 유저 삭제 성공
    @Test
    @DisplayName("유저 삭제를 검증한다.")
    void delete_success() {
        // given
        UUID userId = UUID.randomUUID();

        given(userRepository.existsById(userId)).willReturn(true);

        // when
        Throwable result = catchThrowable(() -> basicUserService.delete(userId));

        // then
        assertThat(result).doesNotThrowAnyException();

        then(userRepository).should().existsById(userId);
        then(userRepository).should().deleteById(userId);
    }

}
