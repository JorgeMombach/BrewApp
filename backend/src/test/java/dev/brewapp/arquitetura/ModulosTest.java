package dev.brewapp.arquitetura;

import static org.assertj.core.api.Assertions.assertThat;

import dev.brewapp.BrewAppApplication;
import dev.brewapp.shared.ErroDeNegocio;
import dev.brewapp.shared.unidade.Quantidade;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.NamedInterfaces;

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

    /**
     * Subpacotes do shared são internos para o Modulith; o catálogo de unidades é exposto como interface nomeada
     * porque o domínio de Recipiente, Lote e Adega usa Quantidade e Unidade.
     */
    @Test
    void catalogoDeUnidadesEInterfacePublicaDoShared() {
        NamedInterfaces interfacesDoShared =
                applicationModules.getModuleByName("shared").orElseThrow().getNamedInterfaces();

        assertThat(interfacesDoShared.getByName("unidade"))
                .hasValueSatisfying(namedInterface ->
                        assertThat(namedInterface.contains(Quantidade.class)).isTrue());
        assertThat(interfacesDoShared.getUnnamedInterface().contains(ErroDeNegocio.class))
                .as("o pacote raiz do shared continua exposto")
                .isTrue();
    }
}
