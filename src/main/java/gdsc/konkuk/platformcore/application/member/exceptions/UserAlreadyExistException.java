package gdsc.konkuk.platformcore.application.member.exceptions;

import gdsc.konkuk.platformcore.global.exceptions.BusinessException;
import gdsc.konkuk.platformcore.global.exceptions.CustomErrorCode;

public class UserAlreadyExistException extends BusinessException {

    protected UserAlreadyExistException(CustomErrorCode errorCode, String logMessage) {
        super(errorCode, logMessage);
    }

    public static UserAlreadyExistException of(CustomErrorCode errorCode) {
        return new UserAlreadyExistException(errorCode, errorCode.getLogMessage());

    }
    public static UserAlreadyExistException of(CustomErrorCode errorCode, String causeMember) {
        return new UserAlreadyExistException(errorCode, errorCode.getLogMessage()+" "+causeMember);
    }
}
