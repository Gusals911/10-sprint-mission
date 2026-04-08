package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @NotBlank(message = "사용자명은 비어 있을 수 없습니다.")
    @Size(min = 2, max = 50, message = "사용자명은 2자 이상 50자 이하여야 합니다.")
    String username,
    @NotBlank(message = "이메일은 비어 있을 수 없습니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
    String email,
    @NotBlank(message = "비밀번호는 비어 있을 수 없습니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
    String password
) {

}
