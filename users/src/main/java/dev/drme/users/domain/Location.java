package dev.drme.users.domain;

import java.util.Objects;

public record Location(
        String countryCode,
        String city
) {
    public Location {
        Objects.requireNonNull(countryCode, "location.countryCode cannot be null");
        Objects.requireNonNull(city, "location.city cannot be null");
    }
}
