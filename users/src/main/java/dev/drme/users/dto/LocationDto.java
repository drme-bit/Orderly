package dev.drme.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * API representation of a user location.
 */
public record LocationDto(
        @NotBlank @Size(max = 255) String countryCode,
        @NotBlank @Size(max = 255) String city
) {
}
