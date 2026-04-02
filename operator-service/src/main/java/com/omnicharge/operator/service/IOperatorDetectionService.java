package com.omnicharge.operator.service;

import com.omnicharge.operator.dto.OperatorDetectionResponse;

public interface IOperatorDetectionService {
    
    OperatorDetectionResponse detectOperator(String mobileNumber);
}
