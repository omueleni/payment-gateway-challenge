package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.ServiceUnavailableResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleException(EventProcessingException ex) {
    LOG.error("Exception happened", ex);
    return new ResponseEntity<>(new ErrorResponse("Page not found"),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleException(ValidationException ex) {
    LOG.error("Exception happened", ex);

    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {

    LOG.error("Exception happened", ex);
    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult().getFieldErrors()
        .forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

    return ResponseEntity.badRequest().body(errors);
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<ServiceUnavailableResponse> handle503(ServiceUnavailableException ex) {
    return new ResponseEntity<>(
        new ServiceUnavailableResponse(ex.getMessage(), PaymentStatus.REJECTED.getName()),
        HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(AmountArgumentException.class)
  public ResponseEntity<ErrorResponse> handleAmountArgument(AmountArgumentException ex) {
    return new ResponseEntity<>(
        new ErrorResponse(ex.getMessage()),
        HttpStatus.BAD_REQUEST);
  }


}
