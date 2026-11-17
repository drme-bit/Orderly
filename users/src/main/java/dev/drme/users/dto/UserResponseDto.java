package dev.drme.users.dto;

import dev.drme.users.domain.UserStatus;

import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String firstName,
        String middleName,
        String lastName,
        String email,
        LocationDto location,
        UserStatus status
) {
}
