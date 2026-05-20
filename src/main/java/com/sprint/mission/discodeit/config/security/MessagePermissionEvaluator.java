package com.sprint.mission.discodeit.config.security;

import com.sprint.mission.discodeit.repository.MessageRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessagePermissionEvaluator {

  private final MessageRepository messageRepository;

  public boolean isAuthor(UUID messageId, Authentication authentication) {
    if (!(authentication.getPrincipal() instanceof DiscodeitUserDetails userDetails)) {
      return false;
    }

    // 메시지가 존재하면 작성자 ID와 현재 인증 사용자 ID를 비교
    Optional<UUID> authorId = messageRepository.findAuthorIdById(messageId);
    if (authorId.isPresent()) {
      return authorId.get().equals(userDetails.getUserDto().id());
    }

    // 메시지가 없으면 서비스 본문에서 기존처럼 404로 처리
    return !messageRepository.existsById(messageId);
  }
}
