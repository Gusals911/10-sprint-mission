package com.sprint.mission.discodeit.exception.userstatus;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class UserStatusNotFoundException extends UserStatusException {
    public UserStatusNotFoundException() {
        super(ErrorCode.USER_STATUS_NOT_FOUND);
    }

    private UserStatusNotFoundException(String key, UUID id) {
        super(ErrorCode.USER_STATUS_NOT_FOUND, Map.of(key, id));
    }

    public static UserStatusNotFoundException withId(UUID userStatusId) {
        return new UserStatusNotFoundException("userStatusId", userStatusId);
    }

    public static UserStatusNotFoundException withUserId(UUID userId) {
        return new UserStatusNotFoundException("userId", userId);
    }

    public static UserStatusNotFoundException fromUserId(UUID userId) {
        return withUserId(userId);
    }

    public static UserStatusNotFoundException fromStatusId(UUID userStatusId) {
        return withId(userStatusId);
    }
}
