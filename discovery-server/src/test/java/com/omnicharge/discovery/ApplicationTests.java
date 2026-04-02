package com.omnicharge.discovery;

import com.omnicharge.common.logging.LogEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Basic context load test for Discovery Server.
 * 
 * Mocks LogEventPublisher to avoid requiring RabbitMQ in tests.
 * This is standard practice for infrastructure services.
 */
@SpringBootTest
class ApplicationTests {

	@MockBean
	private LogEventPublisher logEventPublisher;

	@Test
	void contextLoads() {
		// Verifies Spring context loads successfully with mocked dependencies
	}

}
