package dev.brewapp.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.brewapp.shared.persistence.PostgresDeTeste;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresDeTeste.class)
class SegurancaTest {

    private static final String ROTA_PROTEGIDA = "/api/v1/qualquer-rota";
    private static final String ORIGEM_PERMITIDA = "http://localhost:5173";

    private static final EmissorDeTokensDeTeste emissorDeTokensDeTeste = EmissorDeTokensDeTeste.iniciar();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RespostaDeErroDeSeguranca respostaDeErroDeSeguranca;

    @DynamicPropertySource
    static void apontarParaOEmissorDeTeste(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("BREWAPP_OAUTH_ISSUER_URI", emissorDeTokensDeTeste::issuer);
    }

    @AfterAll
    static void encerrarEmissor() {
        emissorDeTokensDeTeste.encerrar();
    }

    @Test
    void healthEhPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void rotaSemTokenRespondeNaoAutenticadoEmProblemDetails() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get(ROTA_PROTEGIDA)));
    }

    @Test
    void outrosEndpointsDoActuatorNaoSaoPublicos() throws Exception {
        esperarNaoAutenticado(mockMvc.perform(get("/actuator/info")));
    }

    @Test
    void tokenValidoPassaPelaSeguranca() throws Exception {
        // Rota inexistente: passar da segurança significa chegar ao 404, e não parar no 401
        mockMvc.perform(get(ROTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, bearer(emissorDeTokensDeTeste.tokenValido())))
                .andExpect(status().isNotFound());
    }

    @Test
    void tokenComOutraAudienceEhRecusado() throws Exception {
        esperarNaoAutenticado(requisitarComToken(emissorDeTokensDeTeste.tokenComAudience("outra-api")));
    }

    @Test
    void tokenDeOutroEmissorEhRecusado() throws Exception {
        esperarNaoAutenticado(requisitarComToken(emissorDeTokensDeTeste.tokenDeOutroEmissor()));
    }

    @Test
    void tokenExpiradoEhRecusado() throws Exception {
        esperarNaoAutenticado(requisitarComToken(emissorDeTokensDeTeste.tokenExpirado()));
    }

    @Test
    void tokenComAssinaturaDesconhecidaEhRecusado() throws Exception {
        esperarNaoAutenticado(requisitarComToken(emissorDeTokensDeTeste.tokenComAssinaturaDesconhecida()));
    }

    @Test
    void acessoNegadoRespondeProblemDetails() throws Exception {
        // Nenhuma rota exige permissão ainda (epic de Identidade): o handler é exercitado diretamente
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", ROTA_PROTEGIDA);
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        respostaDeErroDeSeguranca.handle(mockHttpServletRequest, mockHttpServletResponse, new AccessDeniedException("sem permissão"));

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(403);
        assertThat(mockHttpServletResponse.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(mockHttpServletResponse.getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
        assertThat(mockHttpServletResponse.getContentAsString()).contains("\"code\":\"ACESSO_NEGADO\"");
    }

    @Test
    void preflightDeOrigemPermitidaEhAceito() throws Exception {
        mockMvc.perform(options(ROTA_PROTEGIDA)
                        .header(HttpHeaders.ORIGIN, ORIGEM_PERMITIDA)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, If-Match, X-Organization-Id"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PERMITIDA));
    }

    @Test
    void preflightDeOrigemDesconhecidaEhRecusado() throws Exception {
        mockMvc.perform(options(ROTA_PROTEGIDA)
                        .header(HttpHeaders.ORIGIN, "http://site-malicioso.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private ResultActions requisitarComToken(String token) throws Exception {
        return mockMvc.perform(get(ROTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private static void esperarNaoAutenticado(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.type").value("https://brewapp.dev/erros/nao-autenticado"))
                .andExpect(jsonPath("$.params").isMap())
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(jsonPath("$.errors").isArray());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
