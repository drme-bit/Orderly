package dev.drme.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Clock;

@Configuration
public class SecurityConfig {

    /**
     * Authentication/authorization is intentionally not part of this service:
     * it will be handled by the dedicated auth service.
     * <p>
     * When auth lands (gateway or token filter), replace {@code permitAll()} with
     * real rules and add a JSON {@code AuthenticationEntryPoint}/{@code AccessDeniedHandler}
     * so error bodies keep the {@code ApiError} shape.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
