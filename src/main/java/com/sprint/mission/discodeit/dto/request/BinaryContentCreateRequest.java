package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BinaryContentCreateRequest(
    @NotBlank(message = "파일명은 비어 있을 수 없습니다.")
    @Size(max = 255, message = "파일명은 255자 이하여야 합니다.")
    String fileName,
    @NotBlank(message = "컨텐츠 타입은 비어 있을 수 없습니다.")
    @Size(max = 100, message = "컨텐츠 타입은 100자 이하여햐 합니다.")
    String contentType,
    @NotNull(message = "파일 데이터는 필수입니다.")
    byte[] bytes
) {

}
