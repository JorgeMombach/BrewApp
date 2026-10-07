package dev.brewapp.shared.logging;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * JWT falso, montado em tempo de execução: o formato é o de um token real (Base64URL de JSON, começando com "eyJ"),
 * mas nenhum literal com cara de token fica no código, para não disparar a varredura de segredos do pre-commit.
 * Não é assinado nem validável; para tokens válidos, ver EmissorDeTokensDeTeste.
 */
final class JwtFalsoDeTeste {

    private JwtFalsoDeTeste() {
    }

    static String gerar() {
        Base64.Encoder base64Url = Base64.getUrlEncoder().withoutPadding();
        String cabecalho = base64Url.encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = base64Url.encodeToString("{\"sub\":\"usuario-de-teste\"}".getBytes(StandardCharsets.UTF_8));
        String assinatura = base64Url.encodeToString("assinatura-falsa".getBytes(StandardCharsets.UTF_8));
        return cabecalho + "." + payload + "." + assinatura;
    }
}
