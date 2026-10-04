package dev.brewapp.shared.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Fake do Keycloak para testes: publica o discovery OIDC e o JWKS num servidor HTTP local
 * e assina tokens com uma chave RSA própria. Exercita a validação real do JwtDecoder
 * (assinatura, emissor, destinatário e validade), sem depender do Keycloak do docker-compose.
 */
final class EmissorDeTokensDeTeste {

    static final String AUDIENCE = "brewapp-api";
    private static final String CAMINHO_DO_REALM = "/realms/brewapp";
    private static final Duration VALIDADE = Duration.ofMinutes(5);

    private final HttpServer httpServer;
    private final RSAKey rsaKey;
    private final String issuer;

    private EmissorDeTokensDeTeste(HttpServer httpServer, RSAKey rsaKey) {
        this.httpServer = httpServer;
        this.rsaKey = rsaKey;
        this.issuer = "http://localhost:" + httpServer.getAddress().getPort() + CAMINHO_DO_REALM;
    }

    static EmissorDeTokensDeTeste iniciar() {
        try {
            RSAKey rsaKey = gerarChave();
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            EmissorDeTokensDeTeste emissorDeTokensDeTeste = new EmissorDeTokensDeTeste(httpServer, rsaKey);
            httpServer.createContext(CAMINHO_DO_REALM + "/.well-known/openid-configuration",
                    httpExchange -> responderJson(httpExchange, emissorDeTokensDeTeste.discovery()));
            httpServer.createContext(CAMINHO_DO_REALM + "/protocol/openid-connect/certs",
                    httpExchange -> responderJson(httpExchange, new JWKSet(rsaKey.toPublicJWK()).toString()));
            httpServer.start();
            return emissorDeTokensDeTeste;
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    String issuer() {
        return issuer;
    }

    void encerrar() {
        httpServer.stop(0);
    }

    String tokenValido() {
        return assinar(rsaKey, issuer, AUDIENCE, Instant.now().plus(VALIDADE));
    }

    String tokenComAudience(String audience) {
        return assinar(rsaKey, issuer, audience, Instant.now().plus(VALIDADE));
    }

    String tokenDeOutroEmissor() {
        return assinar(rsaKey, "http://localhost:1/realms/outro", AUDIENCE, Instant.now().plus(VALIDADE));
    }

    String tokenExpirado() {
        return assinar(rsaKey, issuer, AUDIENCE, Instant.now().minus(Duration.ofHours(1)));
    }

    /** Mesmo emissor e destinatário, mas assinado por uma chave que não está no JWKS. */
    String tokenComAssinaturaDesconhecida() {
        return assinar(gerarChave(), issuer, AUDIENCE, Instant.now().plus(VALIDADE));
    }

    private String discovery() {
        return """
                {"issuer":"%s","jwks_uri":"%s/protocol/openid-connect/certs","subject_types_supported":["public"]}"""
                .formatted(issuer, issuer);
    }

    private static String assinar(RSAKey rsaKeyDeAssinatura, String issuer, String audience, Instant expiracao) {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("usuario-de-teste")
                .issueTime(Date.from(expiracao.minus(VALIDADE)))
                .expirationTime(Date.from(expiracao))
                .build();
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKeyDeAssinatura.getKeyID()).build();
        SignedJWT signedJwt = new SignedJWT(jwsHeader, jwtClaimsSet);
        try {
            signedJwt.sign(new RSASSASigner(rsaKeyDeAssinatura));
        } catch (JOSEException joseException) {
            throw new IllegalStateException(joseException);
        }
        return signedJwt.serialize();
    }

    private static RSAKey gerarChave() {
        try {
            return new RSAKeyGenerator(2048).keyID("chave-de-teste").generate();
        } catch (JOSEException joseException) {
            throw new IllegalStateException(joseException);
        }
    }

    private static void responderJson(HttpExchange httpExchange, String json) throws IOException {
        byte[] corpo = json.getBytes(StandardCharsets.UTF_8);
        httpExchange.getResponseHeaders().set("Content-Type", "application/json");
        httpExchange.sendResponseHeaders(200, corpo.length);
        try (OutputStream outputStream = httpExchange.getResponseBody()) {
            outputStream.write(corpo);
        }
    }
}
