package dev.drme.users.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates service-to-service calls to {@code /internal/**}.
 * <p>
 * An invalid token does not write a response itself: it simply leaves the request
 * anonymous, so the authorization rule and the JSON entry point produce a 401.
 */
final class InternalTokenFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Internal-Token";
    static final String AUTHORITY = "SCOPE_internal";

    private static final Logger log = LoggerFactory.getLogger(InternalTokenFilter.class);

    private final byte[] expectedToken;

    InternalTokenFilter(InternalAuthProperties properties) {
        this.expectedToken = properties.token().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String providedToken = request.getHeader(HEADER);

        if (providedToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!MessageDigest.isEqual(expectedToken, providedToken.getBytes(StandardCharsets.UTF_8))) {
            // Never log the token itself, only the fact that it was rejected.
            log.warn("Rejected internal request with invalid token from {}", request.getRemoteAddr());
            filterChain.doFilter(request, response);
            return;
        }

        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "internal-service",
                null,
                List.of(new SimpleGrantedAuthority(AUTHORITY))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
