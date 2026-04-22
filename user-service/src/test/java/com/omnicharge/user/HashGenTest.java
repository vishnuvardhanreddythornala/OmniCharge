package com.omnicharge.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HashGenTest {
    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("====== GENERATED HASH: " + encoder.encode("Login@630") + " ======");
    }
}
