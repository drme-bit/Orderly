package dev.drme.users.domain;

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DELETED,
    BLOCKED;

    public boolean canTransitionTo(UserStatus target) {
        return switch (this) {
            case ACTIVE -> target != ACTIVE;
            case SUSPENDED -> target == ACTIVE || target == BLOCKED || target == DELETED;
            case INACTIVE, BLOCKED -> target == ACTIVE || target == DELETED;
            case DELETED -> false;
        };
    }
}
