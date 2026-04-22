package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.List;
import java.util.UUID;

public class InvalidChannelParticipantsException extends ChannelException {

  public InvalidChannelParticipantsException() {
    super(ErrorCode.INVALID_REQUEST);
  }

  public static InvalidChannelParticipantsException withParticipantIds(List<UUID> participantIds) {
    InvalidChannelParticipantsException exception = new InvalidChannelParticipantsException();
    exception.addDetail("participantIds", participantIds);
    return exception;
  }
}
