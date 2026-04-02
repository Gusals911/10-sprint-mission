package com.sprint.mission.discodeit.exception.binaryContent;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class BinaryContentStorageException extends BinaryContentException {

    public BinaryContentStorageException(String operation, String target) {
        super(
                ErrorCode.BINARY_CONTENT_STORAGE,
                Map.of(
                        "operation", operation,
                        "target", target
                )
        );
    }
}
