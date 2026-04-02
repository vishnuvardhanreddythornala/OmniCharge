package com.omnicharge.operator.controller;

import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.dto.OperatorDetectionResponse;
import com.omnicharge.operator.dto.OperatorResponse;
import com.omnicharge.operator.service.IOperatorDetectionService;
import com.omnicharge.operator.service.IOperatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OperatorDetectionController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypass Security filters
class OperatorDetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IOperatorDetectionService operatorDetectionService;

    @MockBean
    private IOperatorService operatorService;

    @MockBean
    private LogEventPublisher logEventPublisher;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    private OperatorDetectionResponse operatorDetectionResponse;
    private OperatorResponse operatorResponse;

    @BeforeEach
    void setUp() {
        operatorDetectionResponse = OperatorDetectionResponse.builder()
                .operatorId(1L)
                .operatorCode("AIRTEL")
                .operatorName("Airtel")
                .logoUrl("http://logo.com")
                .plans(Collections.emptyList())
                .build();

        operatorResponse = OperatorResponse.builder()
                .id(1L)
                .code("AIRTEL")
                .name("Airtel")
                .build();
    }

    @Test
    void detectOperator_Success() throws Exception {
        when(operatorDetectionService.detectOperator("9876543210")).thenReturn(operatorDetectionResponse);

        mockMvc.perform(get("/api/operators/detect")
                .param("mobileNumber", "9876543210")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.operatorCode").value("AIRTEL"));
    }

    @Test
    void detectOperator_NotFound() throws Exception {
        when(operatorDetectionService.detectOperator("0000000000")).thenReturn(null);

        mockMvc.perform(get("/api/operators/detect")
                .param("mobileNumber", "0000000000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // The controller code maps this explicitly to HTTP 200 with success = false
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Could not detect operator for the given mobile number"));
    }

    @Test
    void getActiveOperators_Success() throws Exception {
        when(operatorService.getActiveOperators()).thenReturn(List.of(operatorResponse));

        mockMvc.perform(get("/api/operators/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("AIRTEL"));
    }

    @Test
    void getOperatorById_Success() throws Exception {
        when(operatorService.getActiveOperatorById(1L)).thenReturn(operatorResponse);

        mockMvc.perform(get("/api/operators/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Airtel"));
    }
}
