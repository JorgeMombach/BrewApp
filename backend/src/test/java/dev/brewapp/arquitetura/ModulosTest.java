package dev.brewapp.arquitetura;

import dev.brewapp.BrewAppApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Fronteiras entre módulos (spec 4.2): cada subpacote direto de dev.brewapp é um módulo.
 * Falha com dependência circular ou acesso a pacote interno de outro módulo.
 */
class ModulosTest {

    private final ApplicationModules applicationModules = ApplicationModules.of(BrewAppApplication.class);

    @Test
    void modulosRespeitamAsFronteiras() {
        applicationModules.verify();
    }
}
