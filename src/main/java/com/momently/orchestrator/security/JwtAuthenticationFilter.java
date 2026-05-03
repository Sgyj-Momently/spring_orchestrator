package com.momently.orchestrator.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization Bearer JWT를 검사해 보안 컨텍스트를 채운다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String ROLE_CONSOLE = "ROLE_CONSOLE";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        bearerToken(request).flatMap(jwtService::parseValidClaims).ifPresent(this::setAuthenticationFromClaims);

        filterChain.doFilter(request, response);
    }

    private void setAuthenticationFromClaims(Claims claims) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            claims.getSubject(),
            null,
            List.of(new SimpleGrantedAuthority(ROLE_CONSOLE))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return Optional.empty();
        }
        String token = header.substring("Bearer ".length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
