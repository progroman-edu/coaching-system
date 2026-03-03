// This file verifies that the Spring application context starts successfully.
package com.chesscoach.main;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MainApplicationTests {

    @Test
    void contextLoads() {
    }
}

