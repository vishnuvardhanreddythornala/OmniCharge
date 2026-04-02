package com.omnicharge.user;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import com.omnicharge.common.logging.LogEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@SpringBootTest
@Disabled("Requires MySQL database - skipping for unit tests. Property tests cover all functionality.")
class ApplicationTests {

	@MockBean(name = "redisTemplate")
	private RedisTemplate<String, String> redisTemplate;
	
	@MockBean
	private LogEventPublisher logEventPublisher;
	
	@MockBean
	private RabbitTemplate rabbitTemplate;

	@Test
	void contextLoads() {
	}

}
