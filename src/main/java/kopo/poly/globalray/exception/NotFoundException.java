package kopo.poly.globalray.exception;

// 요청한 데이터가 없을 때 사용 (잘못된 입력값인 IllegalArgumentException(400)과 구분해 404로 응답하기 위함)
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
