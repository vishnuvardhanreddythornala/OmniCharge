package com.omnicharge.operator.dto;

import com.omnicharge.operator.entity.OperatorCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorResponse {

    private Long id;
    private String name;
    private String code;
    private OperatorCategory category;
    private String logoUrl;
    private Boolean isActive;
    private Integer planCount;
}
