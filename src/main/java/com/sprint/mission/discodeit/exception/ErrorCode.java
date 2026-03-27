package com.sprint.mission.discodeit.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    DUPLICATE_USERNAME("U001", "중복된 유저명입니다.", HttpStatus.CONFLICT),
    DUPLICATE_EMAIL("U002", "중복된 이메일입니다.", HttpStatus.CONFLICT),
    USER_NOT_FOUND("U003", "존재하지 않는 유저입니다.", HttpStatus.NOT_FOUND),

    CHANNEL_NOT_FOUND("C001", "존재하지 않는 채널입니다.", HttpStatus.NOT_FOUND),
    PRIVATE_CHANNEL_UPDATE_NOT_ALLOWED("C002", "비공개 채널은 수정할 수 없습니다.", HttpStatus.BAD_REQUEST),

    MESSAGE_NOT_FOUND("M001", "존재하지 않는 메세지입니다.", HttpStatus.NOT_FOUND),

    USER_STATUS_ALREADY_EXISTS("S001", "해당 유저에 대한 상태 객체가 이미 존재합니다.", HttpStatus.CONFLICT),
    USER_STATUS_NOT_FOUND("S002", "해당 상태 객체가 존재하지 않습니다.", HttpStatus.NOT_FOUND),

    READ_STATUS_NOT_FOUND("R001", "해당 읽음 상태 객체가 존재하지 않습니다.", HttpStatus.NOT_FOUND),

    BINARY_CONTENT_NOT_FOUND("B001", "해당 파일이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    BINARY_CONTENT_ALREADY_EXISTS("B002" , "해당 파일이 이미 존재합니다.", HttpStatus.CONFLICT),
    BINARY_CONTENT_STORAGE("B003", "파일 저장/조회중 I/O 작업에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_USERNAME("A001", "로그인에 실패하였습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_PASSWORD("A002", "로그인에 실패하였습니다.", HttpStatus.UNAUTHORIZED),

    INTERNAL_SERVER_ERROR("COMMON-500", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}