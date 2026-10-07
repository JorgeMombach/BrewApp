package dev.brewapp.shared.adapter.out.persistence;

import java.time.Duration;
import org.jooq.ExecuteListenerProvider;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga o registro de consultas lentas quando o limite está definido (em produção, no application-prod.yml).
 * O autoconfigure do jOOQ junta todo bean ExecuteListenerProvider à configuração do DSLContext.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("brewapp.sql.limite-de-consulta-lenta")
class ConfiguracaoDeConsultasLentas {

    @Bean
    ExecuteListenerProvider listenerDeConsultasLentas(@Value("${brewapp.sql.limite-de-consulta-lenta}") Duration limite) {
        return new DefaultExecuteListenerProvider(new ListenerDeConsultasLentas(limite));
    }
}
