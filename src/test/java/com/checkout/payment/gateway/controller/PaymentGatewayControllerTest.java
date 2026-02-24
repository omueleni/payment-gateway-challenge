package com.checkout.payment.gateway.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.AcquiringBank.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.service.AcquiringBankService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;
  @Autowired
  ObjectMapper objectMapper;
  @MockBean
  private AcquiringBankService acquiringBankService;

  private ObjectNode basePayload() {
    ObjectNode json = objectMapper.createObjectNode();
    json.put("card_number", "4532015112830366");
    json.put("expiry_month", 12);
    json.put("expiry_year", 27);
    json.put("cvv", "123");
    json.put("currency", "GBP");
    json.put("amount", 10.60);
    return json;
  }

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Page not found"));
  }


  @Test
  void whenPaymentCardNumberEndsInZeroThenReturn503AndNotCallBank() throws Exception {
    ObjectNode json = basePayload();
    json.put("card_number", "35231212224293930");
    String body = json.toString();

    BankPaymentResponse bankResponse = new BankPaymentResponse(true,
        "6b03b0d9-8053-4da7-87ed-0d27490f23e6");

    when(acquiringBankService.authorise(any())).thenReturn(bankResponse);

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
        .content(body)).andExpect(status().isServiceUnavailable()).andExpect(
        jsonPath("$.message").value("Bank payment unavailable for card number ends with 0"))
        .andExpect(jsonPath("$.status").value("Rejected"));

    verify(acquiringBankService, never()).authorise(any());
  }

  @Test
  void whenPaymentCardNumberEndsInEvenNumberThenReturnUnauthorised() throws Exception {

    BankPaymentResponse bankResponse = new BankPaymentResponse(false, "");

    when(acquiringBankService.authorise(any())).thenReturn(bankResponse);

    ObjectNode json = basePayload();
    json.put("card_number", "35231212224293938");
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Declined"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("3938"))
        .andExpect(jsonPath("$.expiryMonth").value("12"))
        .andExpect(jsonPath("$.expiryYear").value("27"))
        .andExpect(jsonPath("$.currency").value("GBP")).andExpect(jsonPath("$.amount").value(1060));

    verify(acquiringBankService, times(1)).authorise(any());

  }

  @Test
  void whenPaymentCardNumberEndsInOddNumberThenReturnAuthorised() throws Exception {

    BankPaymentResponse bankResponse = new BankPaymentResponse(true,
        "6b03b0d9-8053-4da7-87ed-0d27490f23e6");

    when(acquiringBankService.authorise(any())).thenReturn(bankResponse);

    ObjectNode json = basePayload();
    json.put("card_number", "35231212224293937");
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("6b03b0d9-8053-4da7-87ed-0d27490f23e6"))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("3937"))
        .andExpect(jsonPath("$.expiryMonth").value("12"))
        .andExpect(jsonPath("$.expiryYear").value("27"))
        .andExpect(jsonPath("$.currency").value("GBP")).andExpect(jsonPath("$.amount").value(1060));

    verify(acquiringBankService, times(1)).authorise(any());

  }

  @Test
  void whenPaymentDetailsAreValidItShouldBeAuthorisedAndStoredInPaymentRepository() throws Exception {
    BankPaymentResponse bankResponse = new BankPaymentResponse(true,
        "6b03b0d9-8053-4da7-87ed-0d27490f23e6");

    when(acquiringBankService.authorise(any())).thenReturn(bankResponse);

    ObjectNode json = basePayload();
    json.put("card_number", "35231212224293937");
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("6b03b0d9-8053-4da7-87ed-0d27490f23e6"))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("3937"))
        .andExpect(jsonPath("$.expiryMonth").value("12"))
        .andExpect(jsonPath("$.expiryYear").value("27"))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(1060));

    verify(acquiringBankService, times(1)).authorise(any());

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + "6b03b0d9-8053-4da7-87ed-0d27490f23e6"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("3937"))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(27))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(1060));
  }

  @Test
  void whenPaymentCardExpiryYearAndMonthIsNotInFutureThenReturnBadRequest() throws Exception {
    ObjectNode json = basePayload();
    json.put("expiry_month", 1);
    json.put("expiry_year", 15);
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("MM/YY must be in future"));
  }

  @Test
  void whenPaymentCardExpiryMonthIsNotValidThenReturnBadRequest() throws Exception {
    ObjectNode json = basePayload();
    json.put("expiry_month", 14);
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Expiry month must be between 1 and 12"));
  }

  @Test
  void whenPaymentCardCurrencyIsNotInValidThenReturnBadRequest() throws Exception {
    ObjectNode json = basePayload();
    json.put("currency", "ZAR");
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("currency is not valid"));
  }

  @Test
  void whenPaymentCardNumberIsAlphanumericThenReturnBadRequest() throws Exception {
    ObjectNode json = basePayload();
    json.put("card_number", "352312122242A12B");
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Card Number must be must be digits value"));
  }

  @Test
  void whenPaymentCardNumberIsLessThan14CharactersThenReturnBadRequest() throws Exception {
    ObjectNode json = basePayload();
    json.put("card_number", "352312122242");
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Card Number must be between 14 and 19 digits"));
  }

  @Test
  void whenPaymentCardNumberIsOver19CharactersThenReturnBadRequest() throws Exception {
    ObjectNode json = basePayload();
    json.put("card_number", "35231212224293931212");
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Card Number must be between 14 and 19 digits"));
  }

  @Test
  void whenPaymentCardCVVIsLessThan3CharactersThenReturnBadRequest() throws Exception {

    ObjectNode json = basePayload();
    json.put("cvv", 12);
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("CVV must be between 3 and 4 digit"));
  }

  @Test
  void whenPaymentCardCVVIsOver4CharactersThenReturnBadRequest() throws Exception {
    ObjectNode json = basePayload();
    json.put("cvv", 43321);
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("CVV must be between 3 and 4 digit"));
  }

  @Test
  void whenPaymentCardCVVIsAlphaNumericThenReturnBadRequest() throws Exception {
    ObjectNode json = basePayload();
    json.put("cvv", "A12");
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("CVV must be between 3 and 4 digit"));
  }

  @Test
  void whenPaymentCardCVVIsMissingThenReturnBadRequest() throws Exception {
    ObjectNode json = basePayload();
    json.remove("cvv");
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.cvv").value("3 digit cvv required"));
  }

  @Test
  void whenPaymentCardAmountIsMissingThenReturnBadRequest() throws Exception {
    ObjectNode json = basePayload();
    json.remove("amount");
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.amount").value("amount is required"));
  }

  @Test
  void whenPaymentCardNumberIsMissingThenReturnBadRequest() throws Exception {
    ObjectNode json = basePayload();
    json.remove("card_number");
    String body = json.toString();

    mvc.perform(MockMvcRequestBuilders.post("/payment").contentType(MediaType.APPLICATION_JSON)
        .content(body)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.cardNumber").value("minimum 14 digit card number required"));
  }

}