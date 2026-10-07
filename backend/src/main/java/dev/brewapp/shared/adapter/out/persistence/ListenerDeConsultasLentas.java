package dev.brewapp.shared.adapter.out.persistence;

import java.time.Duration;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registra só as consultas acima do limite (spec 7, produção): tempo de execução e SQL com {@code ?} no lugar dos
 * valores, para não expor dados sensíveis. O tempo vai do início da execução ao fim da leitura do resultado.
 * Uma instância só atende todas as threads: o instante de início fica no próprio ExecuteContext.
 */
class ListenerDeConsultasLentas implements ExecuteListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerDeConsultasLentas.class);

    private static final String CHAVE_DO_INICIO = ListenerDeConsultasLentas.class.getName() + ".inicio";

    private final Duration limite;

    ListenerDeConsultasLentas(Duration limite) {
        if (limite == null || limite.isNegative() || limite.isZero()) {
            throw new IllegalStateException("brewapp.sql.limite-de-consulta-lenta precisa ser maior que zero");
        }
        this.limite = limite;
    }

    @Override
    public void executeStart(ExecuteContext executeContext) {
        // nanoTime mede duração e não depende do relógio de parede: não é caso para o Clock injetado
        executeContext.data(CHAVE_DO_INICIO, System.nanoTime());
    }

    @Override
    public void end(ExecuteContext executeContext) {
        if (!(executeContext.data(CHAVE_DO_INICIO) instanceof Long inicioEmNanos)) {
            return;
        }
        Duration duracao = Duration.ofNanos(System.nanoTime() - inicioEmNanos);
        if (duracao.compareTo(limite) >= 0) {
            // sql() é o texto enviado ao driver: com bind parameters (padrão do jOOQ), os valores ficam como "?"
            LOGGER.warn("Consulta lenta: {} ms (limite {} ms): {}", duracao.toMillis(), limite.toMillis(), executeContext.sql());
        }
    }
}
