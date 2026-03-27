package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;

public class DuplicatedUsernameException extends UserException {
    public DuplicatedUsernameException(String username) {
        super(ErrorCode.DUPLICATE_USERNAME, Map.of("username", username));
    }
}
