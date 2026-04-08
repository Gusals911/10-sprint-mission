package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;

public class DuplicatedEmailException extends UserException {
    public DuplicatedEmailException(String email) {
        super(ErrorCode.DUPLICATE_EMAIL, Map.of("email", email));
    }
}
