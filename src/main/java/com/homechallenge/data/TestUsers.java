package com.homechallenge.data;

import java.time.Instant;

public final class TestUsers {
    public static final String VALID_EMAIL = "johndoe@email.com";
    public static final String VALID_PASSWORD = "123";

    private TestUsers() {
    }

    public static String uniqueRegistrationEmail() {
        return "john.doe." + Instant.now().toEpochMilli() + "@email.com";
    }
}
