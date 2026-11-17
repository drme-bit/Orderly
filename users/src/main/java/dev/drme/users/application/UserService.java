package dev.drme.users.application;

import dev.drme.users.domain.Email;
import dev.drme.users.domain.Location;
import dev.drme.users.domain.PersonName;
import dev.drme.users.domain.UserEntity;
import dev.drme.users.dto.LocationDto;
import dev.drme.users.dto.UserRequestDto;
import dev.drme.users.dto.UserResponseDto;
import dev.drme.users.facade.UserFacade;
import dev.drme.users.persistence.UserJpaEntity;
import dev.drme.users.persistence.UserJpaMapper;
import dev.drme.users.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

@Service
@Transactional(readOnly = true)
public class UserService implements UserFacade {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final Clock clock;

    public UserService(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserJpaMapper::toDomain)
                .map(UserService::toResponse)
                .toList();
    }

    @Override
    public UserResponseDto getUserById(UUID id) {
        return toResponse(findUser(id));
    }

    @Override
    @Transactional
    public UserResponseDto createUser(UserRequestDto request) {
        UserEntity user = UserEntity.register(
                nameOf(request),
                emailOf(request.email()),
                locationOf(request),
                clock
        );

        if (userRepository.existsByEmail(user.getEmail().email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User with email " + user.getEmail().email() + " already exists"
            );
        }

        userRepository.save(UserJpaMapper.toJpa(user));
        log.info("User created: {}", user.getId().id());

        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(UserRequestDto request) {
        UUID id = requireId(request);

        UserJpaEntity entity = findJpa(id);
        UserEntity user = UserJpaMapper.toDomain(entity);

        if (request.email() != null) {
            user.changeEmail(emailOf(request.email()), clock);
        }
        if (request.location() != null) {
            user.relocate(locationOf(request), clock);
        }

        UserJpaMapper.applyTo(user, entity);
        userRepository.save(entity);
        log.info("User updated: {}", id);

        return toResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw notFound(id);
        }

        userRepository.deleteById(id);
        log.info("User deleted: {}", id);
    }

    @Override
    @Transactional
    public void blockUser(UUID id) {
        transition(id, UserEntity::block);
    }

    @Override
    @Transactional
    public void unblockUser(UUID id) {
        transition(id, UserEntity::reactivate);
    }

    @Override
    @Transactional
    public void suspendUser(UUID id) {
        transition(id, UserEntity::suspend);
    }

    @Override
    @Transactional
    public void reactivateUser(UUID id) {
        transition(id, UserEntity::reactivate);
    }

    /**
     * Applies a domain state transition and writes the result back to the same
     * managed entity (so the optimistic-lock version is preserved).
     */
    private void transition(UUID id, BiConsumer<UserEntity, Clock> action) {
        UserJpaEntity entity = findJpa(id);
        UserEntity user = UserJpaMapper.toDomain(entity);

        action.accept(user, clock);

        UserJpaMapper.applyTo(user, entity);
        userRepository.save(entity);
        log.info("User {} transitioned to {}", id, user.getStatus());
    }

    private UserEntity findUser(UUID id) {
        return UserJpaMapper.toDomain(findJpa(id));
    }

    private UserJpaEntity findJpa(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static PersonName nameOf(UserRequestDto request) {
        if (request.firstName() == null || request.lastName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "firstName and lastName are required"
            );
        }

        return new PersonName(request.firstName(), request.middleName(), request.lastName());
    }

    private static Email emailOf(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }

        return new Email(rawEmail);
    }

    private static Location locationOf(UserRequestDto request) {
        if (request.location() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "location is required");
        }

        LocationDto location = request.location();
        return new Location(location.countryCode(), location.city());
    }

    private static UUID requireId(UserRequestDto request) {
        if (request.id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        return request.id();
    }

    private static ResponseStatusException notFound(UUID id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
    }

    private static UserResponseDto toResponse(UserEntity user) {
        return new UserResponseDto(
                user.getId().id(),
                user.getName().first(),
                user.getName().middle(),
                user.getName().last(),
                user.getEmail().email(),
                new LocationDto(user.getLocation().countryCode(), user.getLocation().city()),
                user.getStatus()
        );
    }
}
