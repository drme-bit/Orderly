package dev.drme.users.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * User aggregate root.
 * <p>
 * Deliberately free of persistence concerns: the database representation lives in
 * {@code dev.drme.users.persistence.UserJpaEntity} and is mapped by
 * {@code dev.drme.users.persistence.UserJpaMapper}.
 * <p>
 * A user has the following shape:
 * {@code { "91c01a42-c7d5-41a7-8f81-0e80c84c178a", "John", "Doe", "johndoe@gmail.com", "Spain", "Madrid" }}
 */
public class UserEntity {
    private UserId id;
    private PersonName name;
    private Email email;
    private Location location;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public UserEntity(
            UserId id,
            PersonName name,
            Email email,
            Location location,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.email = Objects.requireNonNull(email, "email cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    /**
     * Registers a brand-new user.
     */
    public static UserEntity register(
            PersonName name,
            Email email,
            Location location,
            Clock clock
    ) {
        Instant now = clock.instant();

        return new UserEntity(
                UserId.generate(),
                name,
                email,
                location,
                UserStatus.ACTIVE,
                now,
                now
        );
    }

    /**
     * Rehydrates an existing user (e.g. from the database).
     */
    public static UserEntity restore(
            UserId id,
            PersonName name,
            Email email,
            Location location,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new UserEntity(
                id,
                name,
                email,
                location,
                status,
                createdAt,
                updatedAt
        );
    }

    public UserEntity changeEmail(Email email, Clock clock) {
        ensureActive();

        Objects.requireNonNull(email, "email cannot be null");
        if (this.email.equals(email)) {
            return this;
        }

        this.email = email;
        touch(clock);

        return this;
    }

    public UserEntity relocate(Location location, Clock clock) {
        ensureActive();

        Objects.requireNonNull(location, "location cannot be null");

        this.location = location;
        touch(clock);

        return this;
    }

    private void ensureActive() {
        if (status != UserStatus.ACTIVE) throw new IllegalStateException("User is not active");
    }

    public void reactivate(Clock clock) {
        transitionTo(UserStatus.ACTIVE, clock);
    }

    public void deactivate(Clock clock) {
        transitionTo(UserStatus.INACTIVE, clock);
    }

    public void suspend(Clock clock) {
        transitionTo(UserStatus.SUSPENDED, clock);
    }

    public void delete(Clock clock) {
        transitionTo(UserStatus.DELETED, clock);
    }

    public void block(Clock clock) {
        transitionTo(UserStatus.BLOCKED, clock);
    }

    private void transitionTo(UserStatus target, Clock clock) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Cannot transition from " + status + " to " + target);
        }

        status = target;
        touch(clock);
    }

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof UserEntity userEntity && id.equals(userEntity.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public UserId getId() {
        return id;
    }

    public PersonName getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public Location getLocation() {
        return location;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
