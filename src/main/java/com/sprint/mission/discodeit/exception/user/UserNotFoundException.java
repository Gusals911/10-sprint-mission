package com.sprint.mission.discodeit.exception.user;

import java.util.UUID;

import com.sprint.mission.discodeit.exception.ErrorCode;

public class UserNotFoundException extends UserException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }

    public UserNotFoundException(UUID userId) {
        this();
        addDetail("userId", userId);
    }
    
    public static UserNotFoundException withId(UUID userId) {
        return new UserNotFoundException(userId);
    }
    
    public static UserNotFoundException withUsername(String username) {
        UserNotFoundException exception = new UserNotFoundException();
        exception.addDetail("username", username);
        return exception;
    }
}
