# Design Consideration

Amount Representation
- Accepting amount as String (major units) to allow input like  "10", "10.60", "0.01"
- Convert immediately to minor units (int) and use minor units everywhere internally and when calling bank
- Use BigDecimal only for parsing
- 2 max decimal place

Validation
-  Validation happen at  first line of service before any business logic
-  @NotNull @NotBlank use spring-boot-starter-validation
-  Controller uses @Valid

Service Design and separation of concerns
- Controller: HTTP mapping + @Valid + returns response
- Validator: business validation 
- PaymentService : orchestration (validate -> convert -> call bank -> map response)
- AcquiringBankService: only transport concern (HTTP call, request/response mapping)

Do not call Bank rule
- Policy of not calling the bank if Card number ends with zero

Mapping 2-digit year + formatting expiry month / year for bank
- Using this century 2000 i.e. 2000 + YY for YYYY year format
- When sending to bank format as MM/YYYY

Currency handling
- Two scale currencies used (GBP, USD and EUR)
