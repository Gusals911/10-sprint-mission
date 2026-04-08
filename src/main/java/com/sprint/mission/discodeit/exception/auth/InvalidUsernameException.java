package com.sprint.mission.discodeit.exception.auth;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;

public class InvalidUsernameException extends AuthException {
    public InvalidUsernameException() {
        super(ErrorCode.INVALID_USERNAME, Map.of());
    }
}
