package kr.antimony.accounts.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

// 형식 검사(@Valid) 밖에서 찾은 입력 오류. errors에 필드별 메시지를 담는다. (@Valid 오류와 같은 응답 형식)
public class InvalidInputException extends ApiException {
    public InvalidInputException(Map<String, String> errors) {
        super(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요.", errors);
    }
}
