package com.omnicharge.operator.dto;

import com.omnicharge.operator.entity.PlanCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {

    private Long id;
    private Long operatorId;
    private String operatorName;
    private String planName;
    private BigDecimal price;
    private Integer validityDays;
    private String dataLimit;
    private String callBenefit;
    private String smsBenefit;
    private String additionalBenefits;
    private PlanCategory category;
    private Boolean isActive;
}
