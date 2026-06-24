package report.butt.mediamanager.advice;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import report.butt.mediamanager.exceptions.RequestNotFoundException;

@RestControllerAdvice
@NullMarked
class RequestNotFoundAdvice {

    @ExceptionHandler(RequestNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @Nullable String requestNotFoundHandler(RequestNotFoundException ex) {
        return ex.getMessage();
    }
}
