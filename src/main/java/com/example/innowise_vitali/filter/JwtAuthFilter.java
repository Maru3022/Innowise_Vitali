package com.example.innowise_vitali.filter;

import com.example.innowise_vitali.auth.grpc.AuthGrpcServiceGrpc;
import com.example.innowise_vitali.auth.grpc.ValidateTokenRequest;
import com.example.innowise_vitali.auth.grpc.ValidateTokenResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @GrpcClient("auth-service")
    private AuthGrpcServiceGrpc.AuthGrpcServiceBlockingStub authStub;

    @Value("${gateway.internal-secret:dev-internal-secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String internalHeader = request.getHeader("X-Internal-Secret");
        if (internalHeader != null && internalHeader.equals(internalSecret)) {
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"));
            var auth = new UsernamePasswordAuthenticationToken("internal-service", null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated internal service call via X-Internal-Secret");
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = header.substring(7);
        try {
            ValidateTokenRequest tokenRequest = ValidateTokenRequest.newBuilder().setToken(jwt).build();
            ValidateTokenResponse tokenResponse = authStub.validateToken(tokenRequest);

            if (tokenResponse.getValid()) {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + tokenResponse.getRole()));
                var auth = new UsernamePasswordAuthenticationToken(tokenResponse.getUsername(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Authenticated user='{}' role='{}'", tokenResponse.getUsername(), tokenResponse.getRole());
            } else {
                log.warn("Invalid JWT token received");
                SecurityContextHolder.clearContext();
            }
        } catch (Exception e) {
            log.error("gRPC token validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}