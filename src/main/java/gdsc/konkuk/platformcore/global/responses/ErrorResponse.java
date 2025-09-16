package gdsc.konkuk.platformcore.global.responses;

import gdsc.konkuk.platformcore.global.exceptions.CustomErrorCode;
import lombok.Getter;

@Getter
public class ErrorResponse extends Response {

    private final String errorCode;

    private ErrorResponse(final String message, final String errorCode) {
        super(false, message);
        this.errorCode = errorCode;
    }

    private ErrorResponse(final CustomErrorCode errorCode) {
        super(false, errorCode.getMessage());
        this.errorCode = errorCode.getName();
    }

    public static ErrorResponse of(CustomErrorCode errorCode) {
        return new ErrorResponse(errorCode);
    }

    public static ErrorResponse of(final String message, final String errorCode) {
        return new ErrorResponse(message, errorCode);
    }
    public static ErrorResponse of(final String message, final CustomErrorCode errorCode) {
        return new ErrorResponse(message, errorCode.getName());
    }
}
