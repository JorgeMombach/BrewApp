package dev.brewapp.shared.time;

import static org.assertj.core.api.Assertions.assertThat;

import dev.brewapp.shared.persistence.PostgresDeTeste;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresDeTeste.class)
class ConfiguracaoDeRelogioTest {

    @Autowired
    private Clock clock;

    @Test
    void clockInjetavelEmUtc() {
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
