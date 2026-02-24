package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.AcquiringBank.BankPaymentRequest;
import com.checkout.payment.gateway.model.AcquiringBank.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.ProcessPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import com.checkout.payment.gateway.utils.AmountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final AcquiringBankService bankClient;
  private final PaymentRequestValidatorService paymentRequestValidatorService;


  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      AcquiringBankService bankClient,
      PaymentRequestValidatorService paymentRequestValidatorService) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
    this.paymentRequestValidatorService = paymentRequestValidatorService;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    System.out.println("Starting get Id");
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(ProcessPaymentRequest paymentRequest) {
    LOG.info("Requesting access to to payment {}", paymentRequest);

    paymentRequestValidatorService.validate(paymentRequest);

    int amountMinor = AmountUtil.toMinorUnits(paymentRequest.getAmount());

    LOG.info("Calling acquiring bank with request {}", paymentRequest);

    BankPaymentResponse response = bankClient.authorise(buildBankPaymentRequest(paymentRequest));

    PostPaymentResponse postPaymentResponse = getPostPaymentResponse(response, paymentRequest);

    paymentsRepository.add(postPaymentResponse);

    return postPaymentResponse;
  }


  private BankPaymentRequest buildBankPaymentRequest(ProcessPaymentRequest paymentRequest) {
    return new BankPaymentRequest(
        paymentRequest.getCardNumber(),
        formatExpiry(paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear()),
        paymentRequest.getCurrency(),
        getMinorUnits(paymentRequest.getAmount()),
        paymentRequest.getCvv()
    );
  }

  private PostPaymentResponse getPostPaymentResponse(BankPaymentResponse bankPaymentResponse,
      ProcessPaymentRequest paymentRequest) {

    String paymentId = bankPaymentResponse.authorized()
        ? bankPaymentResponse.authorization_code()
        : UUID.randomUUID().toString();
    PaymentStatus paymentStatus =
        bankPaymentResponse.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;
    PostPaymentResponse postPaymentResponse = new PostPaymentResponse();
    postPaymentResponse.setId(UUID.fromString(paymentId));
    postPaymentResponse.setStatus(paymentStatus);
    postPaymentResponse.setCardNumberLastFour(maskCard(paymentRequest.getCardNumber()));
    postPaymentResponse.setExpiryMonth(paymentRequest.getExpiryMonth());
    postPaymentResponse.setExpiryYear(paymentRequest.getExpiryYear());
    postPaymentResponse.setCurrency(paymentRequest.getCurrency());
    postPaymentResponse.setAmount(getMinorUnits(paymentRequest.getAmount()));
    return postPaymentResponse;
  }

  private String formatExpiry(int month, int twoDigitYear) {
    int fullYear = 2000 + twoDigitYear;
    return String.format("%02d/%d", month, fullYear);
  }

  private int maskCard(String cardNumber) {
    String last4 = cardNumber.substring(cardNumber.length() - 4);
    return Integer.parseInt(last4);
  }

  private int getMinorUnits(String amount) {
    return AmountUtil.toMinorUnits(amount);
  }

}
