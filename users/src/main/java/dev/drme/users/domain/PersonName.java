package dev.drme.users.domain;

import java.util.Objects;

public record PersonName(
        String first,
        String middle,
        String last
) {
    public PersonName {
        Objects.requireNonNull(first, "name.first cannot be null");
        Objects.requireNonNull(last, "name.last cannot be null");
    }
}
