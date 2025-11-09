package com.ecom.profile.security;

import com.ecom.jwt.blocking.BlockingJwtValidationService;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter
 * 
 * <p>Spring Security filter that extracts JWT from Authorization header,
 * validates it, and creates JwtAuthenticationToken for Spring Security context.
 * 
 * <p>This filter runs before Spring Security's authentication chain.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final BlockingJwtValidationService jwtValidationService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Extract JWT from Authorization header
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            // No JWT token - let Spring Security handle it (will reject if endpoint requires auth)
            log.debug("No Authorization header found, continuing filter chain");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract token
            String token = authorizationHeader.substring(7); // Remove "Bearer " prefix

            // Validate token and extract claims
            JWTClaimsSet claims = jwtValidationService.validateToken(token);
            
            // Extract user context from validated JWT claims (source of truth)
            String userId = jwtValidationService.extractUserId(claims);
            String tenantId = jwtValidationService.extractTenantId(claims);
            List<String> roles = jwtValidationService.extractRoles(claims);

            log.debug("JWT validated successfully: userId={}, tenantId={}, roles={}", userId, tenantId, roles);

            // Create authentication token for Spring Security
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                userId, tenantId, roles, token
            );

            // Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (IllegalArgumentException e) {
            // JWT validation failed - this is an authentication error
            log.warn("JWT validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            // Continue filter chain - Spring Security will handle unauthorized access
            filterChain.doFilter(request, response);
            return;
        } catch (RuntimeException e) {
            // JWT validation failed (e.g., invalid token, expired, etc.)
            log.warn("JWT validation failed: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            // Continue filter chain - Spring Security will handle unauthorized access
            filterChain.doFilter(request, response);
            return;
        } catch (Exception e) {
            // Catch-all for any other checked exceptions during JWT validation
            log.error("Unexpected error during JWT authentication", e);
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // If we get here, authentication was successful
        // Continue filter chain - any exceptions from downstream (like BusinessException)
        // will be handled by GlobalExceptionHandler, not this filter
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Optional: Clear SecurityContext after request completes
            // (Spring Security usually does this automatically, but being explicit)
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip JWT validation for public endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
}

