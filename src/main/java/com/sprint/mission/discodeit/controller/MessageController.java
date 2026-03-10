package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.api.MessageApi;
import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.service.MessageService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/messages")
public class MessageController implements MessageApi {

  private final MessageService messageService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Override
  public ResponseEntity<MessageDto> create(
          @RequestPart("messageCreateRequest") MessageCreateRequest messageCreateRequest,
          @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
  ) {
    List<BinaryContentCreateRequest> attachmentRequests = Optional.ofNullable(attachments)
            .map(files -> files.stream()
                    .map(file -> {
                      try {
                        return new BinaryContentCreateRequest(
                                file.getOriginalFilename(),
                                file.getContentType(),
                                file.getBytes()
                        );
                      } catch (IOException e) {
                        throw new RuntimeException(e);
                      }
                    })
                    .toList())
            .orElse(new ArrayList<>());

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(messageService.create(messageCreateRequest, attachmentRequests));
  }

  @PatchMapping("{messageId}")
  @Override
  public ResponseEntity<MessageDto> update(
          @PathVariable("messageId") UUID messageId,
          @RequestBody MessageUpdateRequest request
  ) {
    return ResponseEntity
            .status(HttpStatus.OK)
            .body(messageService.update(messageId, request));
  }

  @DeleteMapping("{messageId}")
  @Override
  public ResponseEntity<Void> delete(@PathVariable("messageId") UUID messageId) {
    messageService.delete(messageId);
    return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
  }

  @GetMapping
  @Override
  public ResponseEntity<PageResponse<MessageDto>> findAllByChannelId(
          @RequestParam("channelId") UUID channelId,
          Pageable pageable
  ) {
    return ResponseEntity
            .status(HttpStatus.OK)
            .body(messageService.findAllByChannelId(channelId, pageable));
  }
}