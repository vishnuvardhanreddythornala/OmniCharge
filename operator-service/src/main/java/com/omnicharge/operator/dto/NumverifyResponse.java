package com.omnicharge.operator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NumverifyResponse {

    private Boolean valid;
    
    private String number;
    
    @JsonProperty("country_code")
    private String countryCode;
    
    private String carrier;
    
    @JsonProperty("line_type")
    private String lineType;
}
