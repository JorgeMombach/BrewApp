package dev.brewapp.shared.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Partes do contrato OpenAPI que não vêm dos controllers (spec 4.5): identificação, autenticação Bearer e o schema
 * de erro. O documento é exportado para packages/shared, que gera os tipos TypeScript a partir dele.
 */
@Configuration
class ConfiguracaoDoContratoDaApi {

    /** Nome do schema de erro em components.schemas; os controllers o referenciam nas respostas de erro. */
    static final String SCHEMA_DE_ERRO = "ProblemDetails";

    private static final String SCHEMA_DE_ERRO_DE_VALIDACAO = "ErroDeValidacao";
    private static final String ESQUEMA_DE_AUTENTICACAO = "bearer";

    @Bean
    OpenAPI contratoDaApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BrewApp API")
                        .version("v1")
                        .description("Diário de adega para cervejarias. Convenções em docs/api/design-api.md."))
                // URL relativa: o documento não muda conforme o host e a porta de quem o gerou
                .servers(List.of(new Server().url("/")))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_DE_AUTENTICACAO, autenticacaoBearer())
                        .addSchemas(SCHEMA_DE_ERRO, problemDetails())
                        .addSchemas(SCHEMA_DE_ERRO_DE_VALIDACAO, erroDeValidacao()))
                // Deny by default (spec 9.2): toda rota exige o token, salvo as públicas declaradas na segurança
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_DE_AUTENTICACAO));
    }

    /** Token de acesso emitido pelo Keycloak (spec 9.1). */
    private static SecurityScheme autenticacaoBearer() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }

    /** Corpo montado pelo {@link Problemas} (design da API, seção 1.6). Campos do RFC 9457 mais os do BrewApp. */
    private static Schema<Object> problemDetails() {
        return new ObjectSchema()
                .description("Problem Details (RFC 9457) com código de erro estável; o cliente traduz o code, "
                        + "nunca exibe o title")
                .addProperty("type", new StringSchema().format("uri"))
                .addProperty("title", new StringSchema().description("Só para leitura humana em logs"))
                .addProperty("status", new IntegerSchema())
                .addProperty("instance", new StringSchema().format("uri-reference"))
                .addProperty(Problemas.PROPRIEDADE_CODE, new StringSchema().description("Contrato com o cliente"))
                .addProperty(Problemas.PROPRIEDADE_PARAMS, parametrosDoErro())
                .addProperty(
                        Problemas.PROPRIEDADE_CORRELATION_ID,
                        new StringSchema().description("Mesmo valor do cabeçalho X-Correlation-Id da resposta"))
                .addProperty(
                        Problemas.PROPRIEDADE_ERRORS,
                        new ArraySchema()
                                .items(new Schema<>().$ref(SCHEMA_DE_ERRO_DE_VALIDACAO))
                                .description("Campos com problema; vazio fora das falhas de validação"))
                .required(List.of(
                        "type",
                        "title",
                        "status",
                        Problemas.PROPRIEDADE_CODE,
                        Problemas.PROPRIEDADE_PARAMS,
                        Problemas.PROPRIEDADE_CORRELATION_ID,
                        Problemas.PROPRIEDADE_ERRORS));
    }

    /** Item de {@code errors}: { "campo": "capacidade.valor", "code": "DEVE_SER_POSITIVO", "params": {} }. */
    private static Schema<Object> erroDeValidacao() {
        return new ObjectSchema()
                .addProperty(Problemas.PROPRIEDADE_CAMPO, new StringSchema())
                .addProperty(Problemas.PROPRIEDADE_CODE, new StringSchema())
                .addProperty(Problemas.PROPRIEDADE_PARAMS, parametrosDoErro())
                .required(
                        List.of(Problemas.PROPRIEDADE_CAMPO, Problemas.PROPRIEDADE_CODE, Problemas.PROPRIEDADE_PARAMS));
    }

    /** Valores usados pelo cliente para montar a mensagem traduzida do code. */
    private static Schema<Object> parametrosDoErro() {
        return new ObjectSchema().additionalProperties(true);
    }
}
