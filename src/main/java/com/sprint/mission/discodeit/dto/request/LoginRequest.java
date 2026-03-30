package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "사용자명은 비어 있을 수 없습니다.")
    @Size(max = 50, message = "사용자명은 50자 이하여야 합니다.")
    String username,
    @NotBlank(message = "비밀번호는 비어 있을 수 없습니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
    String password
) {

}
