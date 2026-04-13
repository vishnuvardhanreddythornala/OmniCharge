package com.omnicharge.operator.controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.dto.OperatorDetectionResponse;
import com.omnicharge.operator.dto.OperatorResponse;
import com.omnicharge.operator.service.IOperatorDetectionService;
import com.omnicharge.operator.service.IOperatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OperatorDetectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class OperatorDetectionControllerTest {
    @MockitoBean(name="logEventPublisher")
    private com.omnicharge.operator.common.logging.LogEventPublisher logEventPublisher;


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IOperatorDetectionService operatorDetectionService;

    @MockBean
    private IOperatorService operatorService;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void detectOperator_Success() throws Exception {
        OperatorDetectionResponse mockResp = OperatorDetectionResponse.builder().operatorName("Airtel").build();
        when(operatorDetectionService.detectOperator(anyString())).thenReturn(mockResp);

        mockMvc.perform(get("/api/operators/detect")
                .param("mobileNumber", "9999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.operatorName").value("Airtel"));
    }

    @Test
    void detectOperator_NotFound() throws Exception {
        when(operatorDetectionService.detectOperator(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/operators/detect")
                .param("mobileNumber", "000"))
                .andExpect(status().isOk()) // App returns 200 OK with ApiResponse.error
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Could not detect operator for the given mobile number"));
    }

    @Test
    void getActiveOperators_Success() throws Exception {
        OperatorResponse op = OperatorResponse.builder().name("Jio").build();
        when(operatorService.getActiveOperators()).thenReturn(List.of(op));

        mockMvc.perform(get("/api/operators/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Jio"));
    }
}

