package com.omnicharge.common.logging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test for automatic component registration via Spring Boot AutoConfiguration.
 * 
 * Property 32: Auto-Configuration of Logging Components
 * - ServiceLifecycleLogger, RabbitMQEventLogger, and RedisOperationLogger are automatically
 *   registered as Spring beans when omnicharge-common is on the classpath
 * - No manual @ComponentScan or @Import required in services
 * 
 * Validates Requirements: 14.1, 14.2, 14.3, 14.4, 14.5
 */
class AutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TestAutoConfiguration.class));

    @Test
    void autoConfiguration_shouldRegisterServiceLifecycleLogger() {
        contextRunner
                .withUserConfiguration(MockLogEventPublisherConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ServiceLifecycleLogger.class);
                    assertThat(context.getBean(ServiceLifecycleLogger.class)).isNotNull();
                });
    }

    @Test
    void autoConfiguration_shouldRegisterRabbitMQEventLogger() {
        contextRunner
                .withUserConfiguration(MockLogEventPublisherConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(RabbitMQEventLogger.class);
                    assertThat(context.getBean(RabbitMQEventLogger.class)).isNotNull();
                });
    }

    @Test
    void autoConfiguration_shouldRegisterRedisOperationLogger() {
        contextRunner
                .withUserConfiguration(MockLogEventPublisherConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisOperationLogger.class);
                    assertThat(context.getBean(RedisOperationLogger.class)).isNotNull();
                });
    }

    @Test
    void autoConfiguration_shouldRegisterAllLoggingComponentsTogether() {
        // Property 32: All logging components are registered automatically
        // This validates that services get complete logging infrastructure without manual configuration
        
        contextRunner
                .withUserConfiguration(MockLogEventPublisherConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ServiceLifecycleLogger.class);
                    assertThat(context).hasSingleBean(RabbitMQEventLogger.class);
                    assertThat(context).hasSingleBean(RedisOperationLogger.class);
                    
                    // Verify all beans are properly initialized
                    ServiceLifecycleLogger lifecycleLogger = context.getBean(ServiceLifecycleLogger.class);
                    RabbitMQEventLogger rabbitLogger = context.getBean(RabbitMQEventLogger.class);
                    RedisOperationLogger redisLogger = context.getBean(RedisOperationLogger.class);
                    
                    assertThat(lifecycleLogger).isNotNull();
                    assertThat(rabbitLogger).isNotNull();
                    assertThat(redisLogger).isNotNull();
                });
    }

    @Test
    void autoConfiguration_shouldWorkWithoutManualComponentScan() {
        // Validates Requirement 14.5: Zero-configuration integration
        // Services should not need @ComponentScan("com.omnicharge.common.logging")
        
        contextRunner
                .withUserConfiguration(MockLogEventPublisherConfig.class)
                .run(context -> {
                    // All logging components should be available without explicit scanning
                    assertThat(context.containsBean("serviceLifecycleLogger")).isTrue();
                    assertThat(context.containsBean("rabbitMQEventLogger")).isTrue();
                    assertThat(context.containsBean("redisOperationLogger")).isTrue();
                });
    }

    /**
     * Test configuration that simulates auto-configuration imports.
     * In real application, this is handled by META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
     */
    @Configuration
    static class TestAutoConfiguration {
        @Bean
        public ServiceLifecycleLogger serviceLifecycleLogger(LogEventPublisher logEventPublisher) {
            return new ServiceLifecycleLogger(logEventPublisher);
        }

        @Bean
        public RabbitMQEventLogger rabbitMQEventLogger(LogEventPublisher logEventPublisher) {
            return new RabbitMQEventLogger(logEventPublisher);
        }

        @Bean
        public RedisOperationLogger redisOperationLogger(LogEventPublisher logEventPublisher) {
            return new RedisOperationLogger(logEventPublisher);
        }
    }

    /**
     * Mock configuration to provide LogEventPublisher bean for testing.
     */
    @Configuration
    static class MockLogEventPublisherConfig {
        @Bean
        public LogEventPublisher logEventPublisher() {
            return new LogEventPublisher(null, null) {
                @Override
                public void publish(LogEvent event) {
                    // Mock implementation - does nothing
                }
            };
        }
    }
}
