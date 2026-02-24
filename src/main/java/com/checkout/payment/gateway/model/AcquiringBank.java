package com.checkout.payment.gateway.model;

public class AcquiringBank {

  public record BankPaymentRequest(
      String card_number,
      String expiry_date,
      String currency,
      int amount,
      String cvv
  ) {}

  public record BankPaymentResponse(
      boolean authorized,
      String authorization_code
  ) {}

}
