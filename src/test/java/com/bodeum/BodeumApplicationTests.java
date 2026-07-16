package com.bodeum;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
class BodeumApplicationTests {

    @Test
    void contextLoads() {
    }

}
