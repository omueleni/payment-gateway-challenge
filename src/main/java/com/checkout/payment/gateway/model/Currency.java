package com.checkout.payment.gateway.model;

public enum Currency {
  USD("USD", "US Dollar"),
  EUR("EUR", "Euro"),
  GBP("GBP", "British Pound");

  private final String code;
  private final String description;

  Currency(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }
}
