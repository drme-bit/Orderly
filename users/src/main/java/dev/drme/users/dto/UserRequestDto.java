package dev.drme.users.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request payload for creating and updating a user.
 * <p>
 * Validation here is shape-only (format and size). Create/update specific
 * requirements (which fields are mandatory) are enforced by the application layer,
 * because the same payload is used for both operations.
 */
public record UserRequestDto(
        UUID id,

        @Size(max = 255)
        String firstName,

        @Size(max = 255)
        String middleName,

        @Size(max = 255)
        String lastName,

        @Email @Size(max = 255)
        String email,

        @Valid
        LocationDto location
) {
}
