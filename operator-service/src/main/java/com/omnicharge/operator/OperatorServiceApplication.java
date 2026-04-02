package com.omnicharge.operator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = {"com.omnicharge.operator", "com.omnicharge.common"})
@EnableDiscoveryClient
@EnableJpaAuditing
public class OperatorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OperatorServiceApplication.class, args);
    }
}
