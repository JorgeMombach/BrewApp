package dev.brewapp.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.brewapp.shared.persistence.PostgresDeTeste;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Exporta o contrato OpenAPI para packages/shared (spec 4.5), de onde saem os tipos TypeScript. Todo build grava o
 * arquivo; a CI falha se o arquivo gravado diferir do commitado, como na checagem do jOOQ (ADR 0013).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresDeTeste.class)
class ContratoDaApiTest {

    private static final Path CONTRATO_EXPORTADO = Path.of("../packages/shared/openapi/openapi.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contratoEhExportadoParaOPacoteCompartilhado() throws Exception {
        String documentoOpenApi = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode contrato = JsonMapper.builder().build().readTree(documentoOpenApi);
        Files.createDirectories(CONTRATO_EXPORTADO.getParent());
        Files.writeString(CONTRATO_EXPORTADO, formatar(contrato), StandardCharsets.UTF_8);

        assertThat(contrato.at("/info/title").asString()).isEqualTo("BrewApp API");
        assertThat(contrato.at("/components/securitySchemes/bearer/scheme").asString())
                .isEqualTo("bearer");
        assertThat(contrato.at("/security/0/bearer").isArray()).isTrue();
        assertThat(contrato.at("/components/schemas/ProblemDetails/required")
                        .valueStream()
                        .map(JsonNode::asString))
                .contains("code", "params", "correlationId", "errors");
        assertThat(contrato.at("/components/schemas/ErroDeValidacao/required")
                        .valueStream()
                        .map(JsonNode::asString))
                .containsExactly("campo", "code", "params");
    }

    @Test
    void swaggerUiFicaSoNoPerfilDev() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
    }

    /**
     * Mesma saída em qualquer máquina: dois espaços, LF e quebra de linha final (.editorconfig), independente do
     * separador de linha do sistema operacional que rodou o teste.
     */
    private static String formatar(JsonNode contrato) {
        DefaultIndenter indentacao = new DefaultIndenter("  ", "\n");
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter(Separators.createDefaultInstance()
                        .withObjectNameValueSpacing(Separators.Spacing.AFTER)
                        .withObjectEmptySeparator("")
                        .withArrayEmptySeparator(""))
                .withObjectIndenter(indentacao)
                .withArrayIndenter(indentacao);
        return JsonMapper.builder().build().writer().with(prettyPrinter).writeValueAsString(contrato) + "\n";
    }
}
