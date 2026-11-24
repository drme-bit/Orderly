package dev.drme.users.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Secret shared with internal services. Bound from {@code ORDERLY_INTERNAL_TOKEN};
 * there is deliberately no default value — a missing secret must fail startup.
 */
@Validated
@ConfigurationProperties(prefix = "orderly.internal")
public record InternalAuthProperties(

        @NotBlank
        @Size(min = 32, message = "internal token must be at least 32 characters")
        String token
) {
}
