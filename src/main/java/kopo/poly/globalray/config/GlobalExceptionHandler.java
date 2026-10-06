package kopo.poly.globalray.config;

import kopo.poly.globalray.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// 오류 화면을 보여줄 때 HTTP 상태 코드도 실제 오류에 맞게 내려줌 (상태 코드가 200이면 브라우저·검색엔진이 정상 응답으로 오해)
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // 정적 리소스 404 - 로그 없이 조용히 처리
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handle404() {
        return "error/404";
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NotFoundException e) {
        log.warn("NotFound : {}", e.getMessage());
        return "error/404";
    }

    // 아래 Exception 핸들러가 가로채면 500이 되므로 다시 던져 Spring Security가 처리하게 함
    // (비로그인 → 로그인 페이지로 이동, 로그인했지만 권한 없음 → 403)
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException e) {
        throw e;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegal(IllegalArgumentException e, Model model) {
        log.warn("IllegalArgument : {}", e.getMessage());
        model.addAttribute("errorMsg", e.getMessage());
        return "error/400";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception e) {
        log.error("Unhandled exception : {}", e.getMessage(), e);
        return "error/500";
    }
}
