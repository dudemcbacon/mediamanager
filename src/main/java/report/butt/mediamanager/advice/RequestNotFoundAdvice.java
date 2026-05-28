package report.butt.mediamanager.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import report.butt.mediamanager.exceptions.RequestNotFoundException;

@RestControllerAdvice
class RequestNotFoundAdvice {

  @ExceptionHandler(RequestNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  String requestNotFoundHandler(RequestNotFoundException ex) {
    return ex.getMessage();
  }
}
