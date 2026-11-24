package dev.drme.users.api;

import dev.drme.users.dto.UserRequestDto;
import dev.drme.users.dto.UserResponseDto;
import dev.drme.users.facade.UserFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(
        name = "Users API",
        description = "API for managing users"
)
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserFacade userFacade;

    public UserController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @GetMapping()
    public List<UserResponseDto> getAllUsers() {
        return userFacade.getAllUsers();
    }

    @Operation(
            summary = "Get a user by ID",
            description = "Returns a user with the specified ID."
    )
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{id}")
    public UserResponseDto getUserById(@PathVariable UUID id) {
        return userFacade.getUserById(id);
    }

    @Operation(
            summary = "Create a new user",
            description = "Creates a new user with the provided details."
    )
    @ApiResponse(responseCode = "201", description = "User created")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto createUser(@Valid @RequestBody UserRequestDto request) {
        return userFacade.createUser(request);
    }

    @Operation(
            summary = "Update an existing user",
            description = "Updates an existing user with the provided details."
    )
    @ApiResponse(responseCode = "200", description = "User updated")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping
    public UserResponseDto updateUser(@Valid @RequestBody UserRequestDto request) {
        return userFacade.updateUser(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        userFacade.deleteUser(id);
    }

    @PutMapping("/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void blockUser(@PathVariable UUID id) {
        userFacade.blockUser(id);
    }

    @PutMapping("/{id}/unblock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblockUser(@PathVariable UUID id) {
        userFacade.unblockUser(id);
    }

    @PutMapping("/{id}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendUser(@PathVariable UUID id) {
        userFacade.suspendUser(id);
    }

    @PutMapping("/{id}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivateUser(@PathVariable UUID id) {
        userFacade.reactivateUser(id);
    }
}
