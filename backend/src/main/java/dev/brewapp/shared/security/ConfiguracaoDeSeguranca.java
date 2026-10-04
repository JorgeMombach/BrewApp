package dev.brewapp.shared.security;

import dev.brewapp.shared.CabecalhosBrewApp;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Negação por padrão (spec 9.2): só a verificação de saúde é pública; todo o resto exige um JWT válido
 * (assinatura, emissor, destinatário e validade, configurados em spring.security.oauth2.resourceserver.jwt).
 */
@Configuration
@EnableConfigurationProperties(PropriedadesCors.class)
class ConfiguracaoDeSeguranca {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, RespostaDeErroDeSeguranca respostaDeErroDeSeguranca)
            throws Exception {
        return httpSecurity
                .authorizeHttpRequests(requestMatcherRegistry -> requestMatcherRegistry
                        // /actuator/health/** cobre os grupos liveness e readiness, usados por orquestradores
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServerConfigurer -> resourceServerConfigurer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(respostaDeErroDeSeguranca)
                        .accessDeniedHandler(respostaDeErroDeSeguranca))
                .exceptionHandling(exceptionHandlingConfigurer -> exceptionHandlingConfigurer
                        .authenticationEntryPoint(respostaDeErroDeSeguranca)
                        .accessDeniedHandler(respostaDeErroDeSeguranca))
                .cors(Customizer.withDefaults())
                // API sem cookie de sessão (token Bearer): não há o que proteger com CSRF
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagementConfigurer -> sessionManagementConfigurer
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(PropriedadesCors propriedadesCors) {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(propriedadesCors.allowedOrigins());
        // Sem DELETE nem PUT: a API não os usa (design da API, seção 1.8)
        corsConfiguration.setAllowedMethods(Stream.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PATCH, HttpMethod.OPTIONS)
                .map(HttpMethod::name)
                .toList());
        // Cabeçalhos da seção 1.2 do design da API
        corsConfiguration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT_LANGUAGE, HttpHeaders.IF_MATCH,
                CabecalhosBrewApp.X_ORGANIZATION_ID, CabecalhosBrewApp.X_CORRELATION_ID));
        corsConfiguration.setExposedHeaders(List.of(
                HttpHeaders.ETAG, HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER, CabecalhosBrewApp.X_CORRELATION_ID));
        // Token no cabeçalho Authorization, nunca em cookie
        corsConfiguration.setAllowCredentials(false);
        corsConfiguration.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return urlBasedCorsConfigurationSource;
    }
}
