package com.checkout.payment.gateway.model;

public class ServiceUnavailableResponse {
  private final String message;
  private final String status;


  public ServiceUnavailableResponse(String message, String status) {
    this.message = message;
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public String getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return "ErrorResponse{" +
        "message='" + message + '\'' +
        ", status='" + status + '\'' +
        '}';
  }
}
