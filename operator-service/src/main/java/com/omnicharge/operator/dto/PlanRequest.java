package com.omnicharge.operator.dto;

import com.omnicharge.operator.entity.PlanCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequest {

    @NotBlank(message = "Plan name is required")
    private String planName;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Validity days is required")
    @Positive(message = "Validity days must be positive")
    private Integer validityDays;

    private String dataLimit;

    private String callBenefit;

    private String smsBenefit;

    private String additionalBenefits;

    @NotNull(message = "Category is required")
    private PlanCategory category;
}
