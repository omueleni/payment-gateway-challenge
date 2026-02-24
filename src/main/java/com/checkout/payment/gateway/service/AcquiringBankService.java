package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.AcquiringBank.BankPaymentRequest;
import com.checkout.payment.gateway.model.AcquiringBank.BankPaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AcquiringBankService {

  private final WebClient webClient;

  public AcquiringBankService(
    WebClient.Builder webClientBuilder,
    @Value("${acquiring.bank.base-url}") String baseUrl
  ){
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  public BankPaymentResponse authorise(BankPaymentRequest request) {
      return webClient.post()
          .uri("/payments")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .retrieve()
          .bodyToMono(BankPaymentResponse.class)
          .block();
  }
}
