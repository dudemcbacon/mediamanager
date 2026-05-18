package report.butt.mediamanager.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import report.butt.mediamanager.exceptions.MovieRequestNotFoundException;

@RestControllerAdvice
class EmployeeNotFoundAdvice {

  @ExceptionHandler(MovieRequestNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  String movieRequestNotFoundHandler(MovieRequestNotFoundException ex) {
    return ex.getMessage();
  }
}
