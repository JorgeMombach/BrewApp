package dev.brewapp.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.brewapp.shared.persistence.PostgresDeTeste;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cabeçalho escrito por extenso de propósito: o teste fixa o contrato (design da API, 1.2).
 * A rota sem token cai no 401 do Spring Security, o que prova que o filtro roda antes da segurança.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@Import({FiltroDeCorrelacaoTest.ControladorDeTeste.class, PostgresDeTeste.class})
class FiltroDeCorrelacaoTest {

    private static final String CABECALHO = "X-Correlation-Id";
    private static final String ROTA_PROTEGIDA = "/api/v1/qualquer-rota";
    private static final String ROTA_QUE_LOGA = "/teste/correlacao";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void identificadorEnviadoPeloClienteEhReaproveitadoNaRespostaENoCorpoDoErro() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA).header(CABECALHO, "celular-sync.0192-abc_1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(CABECALHO, "celular-sync.0192-abc_1"))
                .andExpect(jsonPath("$.correlationId").value("celular-sync.0192-abc_1"));
    }

    @Test
    void semIdentificadorOServidorGeraUm() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(ROTA_PROTEGIDA))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String correlationIdGerado = mvcResult.getResponse().getHeader(CABECALHO);
        assertThat(correlationIdGerado).isNotNull();
        assertThat(UUID.fromString(correlationIdGerado)).isNotNull();
        assertThat(mvcResult.getResponse().getContentAsString()).contains("\"correlationId\":\"" + correlationIdGerado + "\"");
    }

    @Test
    void identificadorForaDoFormatoEhSubstituido() throws Exception {
        String comQuebraDeLinha = "abc\nINFO falso no log";
        String longoDemais = "a".repeat(65);

        for (String correlationIdInvalido : new String[] {comQuebraDeLinha, longoDemais, "com espaço", ""}) {
            MvcResult mvcResult = mockMvc.perform(get(ROTA_PROTEGIDA).header(CABECALHO, correlationIdInvalido)).andReturn();

            String correlationIdDaResposta = mvcResult.getResponse().getHeader(CABECALHO);
            assertThat(correlationIdDaResposta).isNotEqualTo(correlationIdInvalido);
            assertThat(UUID.fromString(correlationIdDaResposta)).isNotNull();
        }
    }

    @Test
    void identificadorApareceNosLogsDaRequisicao(CapturedOutput capturedOutput) throws Exception {
        mockMvc.perform(get(ROTA_QUE_LOGA).with(jwt()).header(CABECALHO, "rastro-123"))
                .andExpect(status().isOk());

        // Vale para o padrão de texto ("[rastro-123] ...") e para o JSON (campo correlationId)
        assertThat(capturedOutput.getOut().lines().filter(linha -> linha.contains("Processando a rota de teste")))
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("rastro-123");
    }

    @Test
    void mdcEhLimpoAoFimDaRequisicao() throws Exception {
        // O MockMvc roda na mesma thread do teste, como o Tomcat reaproveita as threads do pool
        mockMvc.perform(get(ROTA_PROTEGIDA).header(CABECALHO, "nao-pode-vazar"));

        assertThat(MDC.get("correlationId")).isNull();
    }

    @RestController
    static class ControladorDeTeste {

        private static final Logger LOGGER = LoggerFactory.getLogger(ControladorDeTeste.class);

        @GetMapping(ROTA_QUE_LOGA)
        void logar() {
            LOGGER.info("Processando a rota de teste");
        }
    }
}
