package com.gopesh.identity.security;

import com.gopesh.identity.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates the JWT Bearer token on every request, populates the
 * {@link SecurityContextHolder} with the authenticated principal,
 * and cross-checks the token's tenant claim against the request's tenant.
 *
 * <p>Tenant mismatch (e.g. a Sky token used on a Rogers endpoint)
 * results in a 403 — preventing cross-tenant token replay attacks.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = extractBearerToken(request);

        if (token != null) {
            try {
                Claims claims = tokenProvider.validateAndExtract(token);
                String userId = claims.getSubject();
                String tokenTenant = claims.get("tenantId", String.class);
                String role = claims.get("role", String.class);

                String requestTenant = TenantContext.getTenantId();
                if (requestTenant != null && !requestTenant.equals(tokenTenant)) {
                    log.warn("Tenant mismatch — token: {}, request: {}", tokenTenant, requestTenant);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token tenant mismatch");
                    return;
                }

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException ex) {
                log.debug("Expired token for user: {}", ex.getClaims().getSubject());
            } catch (JwtException ex) {
                log.warn("Invalid JWT: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
