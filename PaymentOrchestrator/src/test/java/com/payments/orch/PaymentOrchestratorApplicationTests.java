package com.payments.orch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Dummy Auth0 M2M credentials: the real ones come from the environment (.env),
// which tests must not depend on.
@SpringBootTest(properties = {
		"AUTH0_M2M_CLIENT_ID=test-client-id",
		"AUTH0_M2M_CLIENT_SECRET=test-client-secret"
})
class PaymentOrchestratorApplicationTests {

	@Test
	void contextLoads() {
	}

}
