package dev.brewapp.shared.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Respostas 401 e 403 geradas pelo filtro de segurança, antes de chegar a um controller.
 * Os handlers do Bearer Token preenchem o status e o cabeçalho WWW-Authenticate (RFC 6750);
 * o corpo em Problem Details vem do TratadorGlobalDeErros, via HandlerExceptionResolver.
 */
@Component
class RespostaDeErroDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final AuthenticationEntryPoint bearerTokenAuthenticationEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final AccessDeniedHandler bearerTokenAccessDeniedHandler = new BearerTokenAccessDeniedHandler();
    private final HandlerExceptionResolver handlerExceptionResolver;

    RespostaDeErroDeSeguranca(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                         AuthenticationException authenticationException) throws IOException, ServletException {
        bearerTokenAuthenticationEntryPoint.commence(httpServletRequest, httpServletResponse, authenticationException);
        handlerExceptionResolver.resolveException(httpServletRequest, httpServletResponse, null, authenticationException);
    }

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        bearerTokenAccessDeniedHandler.handle(httpServletRequest, httpServletResponse, accessDeniedException);
        handlerExceptionResolver.resolveException(httpServletRequest, httpServletResponse, null, accessDeniedException);
    }
}
