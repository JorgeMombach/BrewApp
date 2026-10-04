package dev.brewapp.shared.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Único ponto em que o relógio do sistema é criado (regra ArchUnit). Quem precisa do tempo recebe o
 * Clock por injeção; nos testes, um Clock.fixed deixa tudo determinístico (spec 10).
 */
@Configuration
class ConfiguracaoDeRelogio {

    /** Em UTC: o servidor sempre responde em UTC e o cliente converte para o fuso do usuário (design da API, 1.3). */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
