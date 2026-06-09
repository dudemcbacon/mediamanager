package report.butt.mediamanager.security;

/**
 * The two access tiers. Stored on {@link AppUser#getRole()} as the bare name and exposed to Spring Security as
 * {@code ROLE_<name>}.
 */
public enum Role {
    ADMIN,
    USER
}
