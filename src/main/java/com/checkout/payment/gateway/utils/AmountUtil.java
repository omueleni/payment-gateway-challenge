package com.checkout.payment.gateway.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AmountUtil {
  public static int toMinorUnits(String amountStr) {
    try{
      BigDecimal amount = new BigDecimal(amountStr.trim());
      if (amount.signum() <= 0) {
        throw new IllegalArgumentException("Amount must be greater than 0");
      }

      if (amount.scale() > 2) {
        throw new IllegalArgumentException("Amount must have max 2 decimal places");
      }

      return amount
          .movePointRight(2)
          .setScale(0, RoundingMode.UNNECESSARY)
          .intValueExact();
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Amount must be a valid number");
    }
  }
}
