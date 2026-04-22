package com.omnicharge.operator.controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import com.omnicharge.operator.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.service.PlanQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlanController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlanControllerTest {
    @MockitoBean(name="logEventPublisher")
    private com.omnicharge.operator.common.logging.LogEventPublisher logEventPublisher;
    private final MockMvc mockMvc;


    @Autowired
    public PlanControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }
    @MockBean
    private PlanQueryService planQueryService;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void getPlanById_Success() throws Exception {
        PlanResponse plan = PlanResponse.builder().id(1L).planName("Super Plan").build();
        when(planQueryService.getPlanById(1L)).thenReturn(plan);

        mockMvc.perform(get("/api/plans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planName").value("Super Plan"));
    }

    @Test
    void getPlanById_NotFound()  throws Exception {
        when(planQueryService.getPlanById(99L)).thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/plans/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void searchPlans_Success() throws Exception {
        PlanResponse p = PlanResponse.builder().id(1L).planName("Test Plan").build();
        Page<PlanResponse> page = new PageImpl<>(List.of(p));

        when(planQueryService.searchPlansFromRedis(eq(1L), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/plans/search")
                .param("operatorId", "1")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].planName").value("Test Plan"));
    }
}

