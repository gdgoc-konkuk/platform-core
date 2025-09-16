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
    // 유저 정보 중복시 어떤 정보가 중복되었는지 로그에 남기기 위한 메서드
    public static UserAlreadyExistException of(CustomErrorCode errorCode, String causeMember) {
        return new UserAlreadyExistException(errorCode, errorCode.getLogMessage()+". "+causeMember);
    }
}
