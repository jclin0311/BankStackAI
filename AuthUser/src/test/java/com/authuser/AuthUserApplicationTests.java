package com.authuser;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Dummy Auth0 management credentials: these have no default in application-dev.yml
// and normally come from the environment (.env), which tests must not depend on.
@SpringBootTest(properties = {
		"AUTH0_MGMT_CLIENT_ID=test-client-id",
		"AUTH0_MGMT_CLIENT_SECRET=test-client-secret"
})
class AuthUserApplicationTests {

	@Test
	void contextLoads() {
	}

}
