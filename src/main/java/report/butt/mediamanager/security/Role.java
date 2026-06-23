package report.butt.mediamanager.security;

import org.jspecify.annotations.NullMarked;

/**
 * The two access tiers. Stored on {@link AppUser#getRole()} as the bare name and exposed to Spring Security as
 * {@code ROLE_<name>}.
 */
@NullMarked
public enum Role {
    ADMIN,
    USER
}
