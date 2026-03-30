package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicChannelUpdateRequest(
    @NotBlank(message = "수정할 채널 이름은 비어 있을 수 없습니다.")
    @Size(max = 100, message = "채널 이름은 100자 이하여야 합니다.")
    String newName,
    @NotBlank(message = "수정할 채널 설명은 비어 있을 수 없습니다.")
    @Size(max = 255, message = "채널 설명은 255자 이하여야 합니다.")
    String newDescription
) {

}
