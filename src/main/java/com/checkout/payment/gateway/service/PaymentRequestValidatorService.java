package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.ServiceUnavailableException;
import com.checkout.payment.gateway.exception.ValidationException;
import com.checkout.payment.gateway.model.Currency;
import com.checkout.payment.gateway.model.ProcessPaymentRequest;
import org.springframework.stereotype.Service;
import java.time.YearMonth;

@Service
public class PaymentRequestValidatorService {

  public void validate(ProcessPaymentRequest paymentRequest) {
    if (!isDigit(paymentRequest.getCardNumber())) {
      throw new ValidationException("cardNumber", "Card Number must be must be digits value");
    }

    if (paymentRequest.getCardNumber().length() < 14
        || paymentRequest.getCardNumber().length() > 19) {
      throw new ValidationException("cardNumber", "Card Number must be between 14 and 19 digits");
    }

    if (paymentRequest.getExpiryMonth() < 1 || paymentRequest.getExpiryMonth() > 12) {
      throw new ValidationException("expiryMonth", "Expiry month must be between 1 and 12");
    }

    if (!isMonthAndYearInFuture(paymentRequest.getExpiryYear(), paymentRequest.getExpiryMonth())) {
      throw new ValidationException("expiryMonth/expiryYear", "MM/YY must be in future");
    }

    if (!isCurrencyValid(paymentRequest.getCurrency())) {
      throw new ValidationException("currency", "currency is not valid");
    }

    if (!paymentRequest.getCvv().matches("\\d{3,4}")) {
      throw new ValidationException("cvv", "CVV must be between 3 and 4 digit");
    }

    if (paymentRequest.getCardNumber().endsWith("0")) {
      throw new ServiceUnavailableException("Bank payment unavailable for card number ends with 0");
    }
  }

  private boolean isDigit(String input) {
    for (char c : input.toCharArray()) {
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }

  private boolean isMonthAndYearInFuture(int year, int month) {
    int yearInYYYY = 2000 + year;
    YearMonth yearMonth = YearMonth.of(yearInYYYY, month);
    return yearMonth
        .isAfter(YearMonth.now());
  }

  private boolean isCurrencyValid(String input) {
    for (Currency currency : Currency.values()) {
      if (input.equals(currency.toString())) {
        return true;
      }
    }
    return false;
  }

}
