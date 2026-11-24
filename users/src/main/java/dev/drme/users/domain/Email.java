package dev.drme.users.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Objects;

public record Email(
        String email
) {
    private static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    public Email {
        Objects.requireNonNull(email, "email cannot be null");

        email = email.trim().toLowerCase(Locale.ROOT);
        if (!email.matches(EMAIL_PATTERN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }
    }
}
