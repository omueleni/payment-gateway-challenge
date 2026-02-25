package com.checkout.payment.gateway.utils;

import com.checkout.payment.gateway.exception.AmountArgumentException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AmountUtil {

  public static int toMinorUnits(String amountStr) {
    try {
      BigDecimal amount = new BigDecimal(amountStr.trim());
      if (amount.signum() <= 0) {
        throw new AmountArgumentException("Amount must be greater than 0");
      }

      if (amount.scale() > 2) {
        throw new AmountArgumentException("Amount must have max 2 decimal places");
      }

      return amount
          .movePointRight(2)
          .setScale(0, RoundingMode.UNNECESSARY)
          .intValueExact();
    } catch (NumberFormatException ex) {
      throw new AmountArgumentException("Amount must be a valid number [i.e. 0.01 , 10, 10.60]");
    }
  }
}
