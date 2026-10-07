package dev.brewapp;

import dev.brewapp.shared.persistence.PostgresDeTeste;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresDeTeste.class)
class BrewAppApplicationTest {

    @Test
    void contextoSobe() {
    }
}
