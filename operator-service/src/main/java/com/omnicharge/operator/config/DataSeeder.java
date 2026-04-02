package com.omnicharge.operator.config;

import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.OperatorCategory;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.repository.OperatorRepository;
import com.omnicharge.operator.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final OperatorRepository operatorRepository;
    private final PlanRepository planRepository;

    @Override
    public void run(String... args) {
        seedOperators();
        seedPlans();
    }

    private void seedOperators() {
        if (operatorRepository.count() > 0) {
            log.info("Operators already seeded");
            return;
        }

        List<Operator> operators = Arrays.asList(
                createOperator("Airtel", "AIRTEL", OperatorCategory.PREPAID, "https://example.com/airtel-logo.png"),
                createOperator("Jio", "JIO", OperatorCategory.PREPAID, "https://example.com/jio-logo.png"),
                createOperator("Vi", "VI", OperatorCategory.PREPAID, "https://example.com/vi-logo.png"),
                createOperator("BSNL", "BSNL", OperatorCategory.PREPAID, "https://example.com/bsnl-logo.png")
        );

        operatorRepository.saveAll(operators);
        log.info("Seeded {} operators", operators.size());
    }

    private void seedPlans() {
        if (planRepository.count() > 0) {
            log.info("Plans already seeded");
            return;
        }

        Operator airtel = operatorRepository.findByCode("AIRTEL").orElse(null);
        Operator jio = operatorRepository.findByCode("JIO").orElse(null);
        Operator vi = operatorRepository.findByCode("VI").orElse(null);

        if (airtel != null) {
            planRepository.saveAll(Arrays.asList(
                    createPlan(airtel, "Unlimited 84 Days", new BigDecimal("719"), 84, "2GB/day", "Unlimited", "100 SMS/day", "Free Hellotunes", PlanCategory.RECOMMENDED),
                    createPlan(airtel, "Data Booster", new BigDecimal("299"), 28, "1.5GB/day", "Unlimited", "100 SMS/day", "Disney+ Hotstar Mobile", PlanCategory.DATA),
                    createPlan(airtel, "Talktime Special", new BigDecimal("199"), 28, "1GB/day", "Unlimited", "100 SMS/day", null, PlanCategory.TALKTIME)
            ));
        }

        if (jio != null) {
            planRepository.saveAll(Arrays.asList(
                    createPlan(jio, "Jio Unlimited", new BigDecimal("666"), 84, "2GB/day", "Unlimited", "100 SMS/day", "JioTV, JioCinema", PlanCategory.RECOMMENDED),
                    createPlan(jio, "Data Pack", new BigDecimal("349"), 28, "2GB/day", "Unlimited", "100 SMS/day", "JioSaavn Pro", PlanCategory.DATA)
            ));
        }

        if (vi != null) {
            planRepository.saveAll(Arrays.asList(
                    createPlan(vi, "Vi Hero Unlimited", new BigDecimal("699"), 84, "1.5GB/day", "Unlimited", "100 SMS/day", "Vi Movies & TV", PlanCategory.UNLIMITED),
                    createPlan(vi, "Weekend Data", new BigDecimal("249"), 28, "1GB/day", "Unlimited", "100 SMS/day", null, PlanCategory.DATA)
            ));
        }

        log.info("Seeded plans for all operators");
    }

    private Operator createOperator(String name, String code, OperatorCategory category, String logoUrl) {
        Operator operator = new Operator();
        operator.setName(name);
        operator.setCode(code);
        operator.setCategory(category);
        operator.setLogoUrl(logoUrl);
        operator.setIsActive(true);
        return operator;
    }

    private Plan createPlan(Operator operator, String planName, BigDecimal price, Integer validityDays,
                            String dataLimit, String callBenefit, String smsBenefit, 
                            String additionalBenefits, PlanCategory category) {
        Plan plan = new Plan();
        plan.setOperator(operator);
        plan.setPlanName(planName);
        plan.setPrice(price);
        plan.setValidityDays(validityDays);
        plan.setDataLimit(dataLimit);
        plan.setCallBenefit(callBenefit);
        plan.setSmsBenefit(smsBenefit);
        plan.setAdditionalBenefits(additionalBenefits);
        plan.setCategory(category);
        plan.setIsActive(true);
        plan.setDeactivatedByOperator(false); // All seeded plans are manually created, not deactivated by operator
        return plan;
    }
}
