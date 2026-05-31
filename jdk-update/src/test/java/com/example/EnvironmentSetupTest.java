package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnvironmentSetupTest {

    @Test
    void shouldRunOnJava25() {
        int feature = Runtime.version().feature();
        assertEquals(25, feature);
    }
}
