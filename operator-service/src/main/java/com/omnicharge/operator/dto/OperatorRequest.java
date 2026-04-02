package com.omnicharge.operator.dto;

import com.omnicharge.operator.entity.OperatorCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatorRequest {

    @NotBlank(message = "Operator name is required")
    private String name;

    @NotBlank(message = "Operator code is required")
    private String code;

    @NotNull(message = "Category is required")
    private OperatorCategory category;

    private String logoUrl;
}
