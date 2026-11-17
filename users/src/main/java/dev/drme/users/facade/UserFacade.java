package dev.drme.users.facade;

import dev.drme.users.dto.UserRequestDto;
import dev.drme.users.dto.UserResponseDto;

import java.util.List;
import java.util.UUID;

public interface UserFacade {
    List<UserResponseDto> getAllUsers();
    UserResponseDto getUserById(UUID id);
    UserResponseDto createUser(UserRequestDto user);
    UserResponseDto updateUser(UserRequestDto user);
    void deleteUser(UUID id);
    void blockUser(UUID id);
    void unblockUser(UUID id);
    void suspendUser(UUID id);
    void reactivateUser(UUID id);
}
