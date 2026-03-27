package com.sprint.mission.discodeit.exception.binaryContent;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;

public class BinaryContentStorageException extends BinaryContentException {
    public BinaryContentStorageException() {
        super(ErrorCode.BINARY_CONTENT_STORAGE, Map.of());
    }
}
