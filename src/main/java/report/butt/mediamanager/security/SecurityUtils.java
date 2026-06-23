package report.butt.mediamanager.security;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Small read-only helpers for the currently authenticated user, used to gate UI actions by role. */
@NullMarked
public final class SecurityUtils {

    private SecurityUtils() {}

    /** True when the current user has the ADMIN role. */
    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getAuthorities().stream().anyMatch(a -> ("ROLE_" + Role.ADMIN.name()).equals(a.getAuthority()));
    }

    /** The current user's username, or null when unauthenticated. */
    public static @Nullable String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
