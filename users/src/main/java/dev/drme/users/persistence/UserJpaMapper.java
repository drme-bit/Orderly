package dev.drme.users.persistence;

import dev.drme.users.domain.Email;
import dev.drme.users.domain.Location;
import dev.drme.users.domain.PersonName;
import dev.drme.users.domain.UserEntity;
import dev.drme.users.domain.UserId;
import dev.drme.users.domain.UserStatus;

/**
 * Converts between the domain aggregate {@link UserEntity} and its persistence
 * representation {@link UserJpaEntity}.
 */
public final class UserJpaMapper {

    private UserJpaMapper() {
    }

    public static UserJpaEntity toJpa(UserEntity user) {
        UserJpaEntity entity = new UserJpaEntity();
        applyTo(user, entity);
        return entity;
    }

    /**
     * Copies the domain state onto an existing JPA entity.
     * <p>
     * Used for updates so that the managed entity (and its {@code @Version})
     * is preserved instead of replacing it with a detached copy.
     */
    public static void applyTo(UserEntity user, UserJpaEntity entity) {
        entity.setId(user.getId().id());
        entity.setFirstName(user.getName().first());
        entity.setMiddleName(user.getName().middle());
        entity.setLastName(user.getName().last());
        entity.setEmail(user.getEmail().email());
        entity.setCountryCode(user.getLocation().countryCode());
        entity.setCity(user.getLocation().city());
        entity.setStatus(user.getStatus().name());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
    }

    public static UserEntity toDomain(UserJpaEntity entity) {
        return UserEntity.restore(
                new UserId(entity.getId()),
                new PersonName(entity.getFirstName(), entity.getMiddleName(), entity.getLastName()),
                new Email(entity.getEmail()),
                new Location(entity.getCountryCode(), entity.getCity()),
                UserStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
