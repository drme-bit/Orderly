package dev.drme.users.config;

import dev.drme.users.exception.GlobalExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(InternalAuthProperties.class)
public class SecurityConfig {

    /**
     * Service-to-service calls live under a separate prefix and never reach the public
     * chain. The gateway must strip {@code X-Internal-Token} from client requests and
     * must not expose {@code /internal/**} through the public ingress.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(
            HttpSecurity http,
            InternalAuthProperties internalAuth,
            GlobalExceptionHandler errors
    ) throws Exception {
        return http
                .securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new InternalTokenFilter(internalAuth), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasAuthority(InternalTokenFilter.AUTHORITY)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors)
                )
                .build();
    }

    /**
     * Browser clients authenticate with a cookie session established by the auth
     * service/gateway, so mutating requests require a CSRF token. Spring Security 7's
     * {@code spa()} stores it in a JS-readable {@code XSRF-TOKEN} cookie and expects
     * it back in the {@code X-XSRF-TOKEN} header.
     * <p>
     * Authorization is still {@code permitAll()} until user authentication is wired
     * to the auth service; replace it with {@code authenticated()} at that point and
     * keep the JSON entry point/access denied handler for the {@code ApiError} contract.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain publicSecurityFilterChain(
            HttpSecurity http,
            GlobalExceptionHandler errors
    ) throws Exception {
        return http
                .csrf(CsrfConfigurer::spa)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors)
                )
                .build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
