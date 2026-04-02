package com.omnicharge.recharge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RechargeRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number")
    private String mobileNumber;

    @NotNull(message = "Operator ID is required")
    private Long operatorId;

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
}
