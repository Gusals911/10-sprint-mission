package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.mapper.PageResponseMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class BasicMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private BinaryContentStorage binaryContentStorage;
    @Mock
    private BinaryContentRepository binaryContentRepository;
    @Mock
    private PageResponseMapper pageResponseMapper;
    @InjectMocks
    private BasicMessageService basicMessageService;

    // 첨부파일 없이 생성 성공
    @Test
    @DisplayName("첨부파일 없이 메시지 생성만 검증한다.")
    void create_no_attachment_success() {
        // given
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        UUID channelId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        MessageCreateRequest request = new MessageCreateRequest("hello", channelId, authorId);
        List<BinaryContentCreateRequest> emptyAttachments = List.of();
        Channel channel = new Channel(ChannelType.PUBLIC, "backend", "backend channel");
        User author = new User("gusals", "gusals@naver.com", "1234", null);
        MessageDto expected = new MessageDto(UUID.randomUUID(), Instant.now(), Instant.now(), "hello",
                channelId, null, List.of());

        given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
        given(userRepository.findById(authorId)).willReturn(Optional.of(author));
        given(messageMapper.toDto(any())).willReturn(expected);

        // when
        MessageDto result = basicMessageService.create(request, emptyAttachments);

        // then
        assertThat(result).isEqualTo(expected);

        then(channelRepository).should().findById(channelId);
        then(userRepository).should().findById(authorId);
        then(messageRepository).should().save(messageCaptor.capture());

        Message savedMessage = messageCaptor.getValue();

        then(messageMapper).should().toDto(savedMessage);
        then(binaryContentRepository).should(never()).save(any());
        then(binaryContentStorage).should(never()).put(any(), any());

        assertThat(savedMessage.getContent()).isEqualTo("hello");
        assertThat(savedMessage.getChannel()).isEqualTo(channel);
        assertThat(savedMessage.getAuthor()).isEqualTo(author);
        assertThat(savedMessage.getAttachments()).isEmpty();
    }

    // 첨부파일 포함 생성 성공
    @Test
    @DisplayName("첨부파일을 포함한 메시지 생성을 검증한다.")
    void create_with_attachment_success() {
        // given
        ArgumentCaptor<BinaryContent> binaryContentCaptor = ArgumentCaptor.forClass(BinaryContent.class);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        UUID channelId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        MessageCreateRequest request = new MessageCreateRequest("hello", channelId, authorId);
        BinaryContentCreateRequest tempAttachment = new BinaryContentCreateRequest("image.png", "image/png",
                "test".getBytes());
        List<BinaryContentCreateRequest> attachments = List.of(tempAttachment);
        Channel channel = new Channel(ChannelType.PUBLIC, "backend", "backend channel");
        User author = new User("gusals", "gusals@naver.com", "1234", null);
        MessageDto expected = new MessageDto(UUID.randomUUID(), Instant.now(), Instant.now(), "hello",
                channelId, null, List.of());

        given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
        given(userRepository.findById(authorId)).willReturn(Optional.of(author));
        given(messageMapper.toDto(any())).willReturn(expected);

        // when
        MessageDto result = basicMessageService.create(request, attachments);

        // then
        then(messageRepository).should().save(messageCaptor.capture());
        then(binaryContentRepository).should().save(binaryContentCaptor.capture());

        Message savedMessage = messageCaptor.getValue();
        BinaryContent savedBinaryContent = binaryContentCaptor.getValue();

        assertThat(result).isEqualTo(expected);
        assertThat(savedMessage.getAttachments()).hasSize(1);
        assertThat(savedMessage.getAttachments()).contains(savedBinaryContent);

        then(binaryContentStorage).should().put(savedBinaryContent.getId(), tempAttachment.bytes());
    }

    // 채널별 메시지 조회 성공
    @Test
    @DisplayName("채널별 메시지 목록 조회를 검증한다.")
    void find_all_by_channel_id_success() {
        // given
        UUID channelId = UUID.randomUUID();
        Instant cursor = Instant.parse("2026-04-02T10:00:00Z");
        Pageable pageable = PageRequest.of(0, 2);
        Channel channel = new Channel(ChannelType.PUBLIC, "backend", "backend channel");
        User author = new User("gusals", "gusals@naver.com", "1234", null);
        Message firstMessage = new Message("first", channel, author, List.of());
        Message secondMessage = new Message("second", channel, author, List.of());
        MessageDto firstExpected = new MessageDto(UUID.randomUUID(), Instant.parse("2026-04-02T09:58:00Z"),
                Instant.parse("2026-04-02T09:58:00Z"), "first", channelId, null, List.of());
        MessageDto secondExpected = new MessageDto(UUID.randomUUID(), Instant.parse("2026-04-02T09:57:00Z"),
                Instant.parse("2026-04-02T09:57:00Z"), "second", channelId, null, List.of());
        Slice<Message> slice = new SliceImpl<>(List.of(firstMessage, secondMessage), pageable, true);
        PageResponse<MessageDto> expected = new PageResponse<>(List.of(firstExpected, secondExpected),
                secondExpected.createdAt(), pageable.getPageSize(), true, null);

        given(messageRepository.findAllByChannelIdWithAuthor(channelId, cursor, pageable)).willReturn(slice);
        given(messageMapper.toDto(firstMessage)).willReturn(firstExpected);
        given(messageMapper.toDto(secondMessage)).willReturn(secondExpected);
        given(pageResponseMapper.fromSlice(org.mockito.ArgumentMatchers.<Slice<MessageDto>>any(),
                eq(secondExpected.createdAt()))).willReturn(expected);

        // when
        PageResponse<MessageDto> result = basicMessageService.findAllByChannelId(channelId, cursor, pageable);

        // then
        assertThat(result).isEqualTo(expected);

        then(messageRepository).should().findAllByChannelIdWithAuthor(channelId, cursor, pageable);
        then(messageMapper).should().toDto(firstMessage);
        then(messageMapper).should().toDto(secondMessage);
        then(pageResponseMapper).should().fromSlice(org.mockito.ArgumentMatchers.<Slice<MessageDto>>any(),
                eq(secondExpected.createdAt()));
    }

    // 메시지 수정 성공
    @Test
    @DisplayName("메시지 수정을 검증한다.")
    void update_success() {
        // given
        UUID messageId = UUID.randomUUID();
        MessageUpdateRequest request = new MessageUpdateRequest("updated");
        Channel channel = new Channel(ChannelType.PUBLIC, "backend", "backend channel");
        User author = new User("gusals", "gusals@naver.com", "1234", null);
        Message message = new Message("hello", channel, author, List.of());
        MessageDto expected = new MessageDto(messageId, Instant.now(), Instant.now(), "updated",
                UUID.randomUUID(), null, List.of());

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageMapper.toDto(any())).willReturn(expected);

        // when
        MessageDto result = basicMessageService.update(messageId, request);

        // then
        assertThat(result).isEqualTo(expected);
        assertThat(message.getContent()).isEqualTo("updated");

        then(messageRepository).should().findById(messageId);
        then(messageMapper).should().toDto(message);
    }

    // 메시지 삭제 성공
    @Test
    @DisplayName("메시지 삭제를 검증한다.")
    void delete_success() {
        // given
        UUID messageId = UUID.randomUUID();

        given(messageRepository.existsById(messageId)).willReturn(true);

        // when
        Throwable result = catchThrowable(() -> basicMessageService.delete(messageId));

        // then
        assertThat(result).doesNotThrowAnyException();

        then(messageRepository).should().existsById(messageId);
        then(messageRepository).should().deleteById(messageId);
    }
}
