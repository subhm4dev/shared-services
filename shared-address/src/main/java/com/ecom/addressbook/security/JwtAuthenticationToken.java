package com.ecom.addressbook.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Token
 * 
 * <p>Custom Spring Security Authentication object that holds JWT claims.
 * Used for @PreAuthorize method-level security.
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String userId;
    private final String tenantId;
    private final List<String> roles;
    private final String token;

    public JwtAuthenticationToken(String userId, String tenantId, List<String> roles, String token) {
        super(roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList()));
        this.userId = userId;
        this.tenantId = tenantId;
        this.roles = roles;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        // Return userId as principal for @PreAuthorize expressions
        return userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getToken() {
        return token;
    }
}

