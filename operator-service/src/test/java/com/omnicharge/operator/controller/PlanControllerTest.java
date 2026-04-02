package com.omnicharge.operator.controller;

import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.service.PlanQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlanController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypass Security filters
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlanQueryService planQueryService;

    @MockBean
    private LogEventPublisher logEventPublisher;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    private PlanResponse planResponse;
    private Page<PlanResponse> planPage;

    @BeforeEach
    void setUp() {
        planResponse = PlanResponse.builder()
                .id(1L)
                .operatorId(1L)
                .operatorName("Airtel")
                .planName("Unlimited 299")
                .price(new BigDecimal("299.00"))
                .validityDays(28)
                .category(PlanCategory.UNLIMITED)
                .isActive(true)
                .build();

        planPage = new PageImpl<>(List.of(planResponse));
    }

    @Test
    void getPlanById_Success() throws Exception {
        when(planQueryService.getPlanById(1L)).thenReturn(planResponse);

        mockMvc.perform(get("/api/plans/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planName").value("Unlimited 299"))
                .andExpect(jsonPath("$.data.price").value(299.00));
    }

    @Test
    void searchPlans_Success() throws Exception {
        // We inject generic matchers due to Pageable request instantiation in the controller
        when(planQueryService.searchPlansFromRedis(eq(1L), any(PlanCategory.class), any(), any(), any(Pageable.class)))
                .thenReturn(planPage);

        mockMvc.perform(get("/api/plans/search")
                .param("operatorId", "1")
                .param("category", "UNLIMITED")
                .param("minPrice", "100.00")
                .param("maxPrice", "500.00")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "price")
                .param("sortDir", "ASC")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].operatorName").value("Airtel"));
    }
}
