package com.cloudledger.account.dto;

import com.cloudledger.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateAccountRequest {

    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotBlank(message = "Customer reference is required")
    private String customerReference;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency must be 3-letter ISO code")
    private String currency;

    public CreateAccountRequest() {}

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getCustomerReference() { return customerReference; }
    public void setCustomerReference(String customerReference) { this.customerReference = customerReference; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
