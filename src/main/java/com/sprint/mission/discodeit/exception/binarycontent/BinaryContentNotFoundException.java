package com.sprint.mission.discodeit.exception.binaryContent;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class BinaryContentNotFoundException extends BinaryContentException {
    public BinaryContentNotFoundException() {
        super(ErrorCode.BINARY_CONTENT_NOT_FOUND);
    }

    public BinaryContentNotFoundException(UUID binaryContentId) {
        super(ErrorCode.BINARY_CONTENT_NOT_FOUND, Map.of("binaryContentId", binaryContentId));
    }

    public static BinaryContentNotFoundException withId(UUID binaryContentId) {
        return new BinaryContentNotFoundException(binaryContentId);
    }
}
