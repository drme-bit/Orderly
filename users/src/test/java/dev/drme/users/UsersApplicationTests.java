package dev.drme.users;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "orderly.internal.token=test-only-internal-token-with-32-bytes")
class UsersApplicationTests {

    @Test
    void contextLoads() {
    }

}
