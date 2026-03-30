package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MessageUpdateRequest(
    @NotBlank(message = "수정할 메세지 내용은 비어 있을 수 없습니다.")
    String newContent
) {

}
