package com.marketplace;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires full application context with database")
class MarketplaceApplicationTests {

	@Test
	void contextLoads() {
	}

}
