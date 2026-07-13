package com.mybill.MyBill_Backend.security;

import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final JwtTokenDenylist tokenDenylist;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if (log.isDebugEnabled()) {
            log.debug("--- JWT FILTER TRIGGERED ---");
            log.debug("Request Path: {}", path);
            log.debug("HTTP Method: {}", request.getMethod());
        }

        if (path.startsWith("/api/auth/") || path.startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (log.isDebugEnabled()) {
            log.debug("Authorization header present: {}", header != null);
        }

        if (header == null || !header.startsWith("Bearer ")) {
            if (log.isDebugEnabled()) log.debug("Action: No valid Bearer token found. Passing to Spring Security as unauthenticated.");
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            if (tokenDenylist.isDenied(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been logged out");
                return;
            }

            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                if (log.isDebugEnabled()) {
                    log.debug("Token Status: VALID");
                }

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var userOpt = userRepository.findByEmail(email);
                    if (userOpt.isEmpty()) {
                        if (log.isWarnEnabled()) {
                            log.warn("Action: Token subject has no matching user. Rejecting request.");
                        }
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User account no longer exists");
                        return;
                    }
                    var user = userOpt.get();
                    String expectedRole = JwtUtil.authorityFor(user.getRole());
                    String tokenRole = jwtUtil.extractRole(token);

                    if (!expectedRole.equals(tokenRole)) {
                        if (log.isWarnEnabled()) {
                            log.warn("Action: Token role does not match persisted role. Rejecting request.");
                        }
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token role is no longer valid");
                        return;
                    }

                    if (log.isDebugEnabled()) log.debug("Action: User exists in DB. Proceeding with authentication.");

                    var authorities = new LinkedHashSet<SimpleGrantedAuthority>();
                    jwtUtil.extractAuthorities(token).stream()
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                    jwtUtil.extractScopes(token).stream()
                            .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                            .forEach(authorities::add);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);

                    // Auditing must obtain the user id without issuing a query
                    // during Hibernate's flush lifecycle. See AuditingConfig.
                    authToken.setDetails(user.getId());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    if (log.isDebugEnabled()) log.debug("Action: Successfully authenticated user in SecurityContext.");
                }
            } else {
                if (log.isDebugEnabled()) log.debug("Token Status: INVALID (Validate method returned false)");
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Token validation exception: exception={} message={}",
                        e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
            }
            // Do not leak exception details to client – will be handled by GlobalExceptionHandler later
        }

        filterChain.doFilter(request, response);
    }
}
