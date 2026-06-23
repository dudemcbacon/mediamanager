package report.butt.mediamanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class AppUserTest {

    @Test
    void constructor_setsFieldsAndEnabledTrue() {
        var user = new AppUser("alice", "hashedPwd", "ADMIN");

        assertEquals("alice", user.getUsername());
        assertEquals("hashedPwd", user.getPasswordHash());
        assertEquals("ADMIN", user.getRole());
        assertTrue(user.isEnabled());
    }

    @Test
    void setters_roundTrip() {
        var user = new AppUser("alice", "hash", "ADMIN");

        user.setUsername("bob");
        assertEquals("bob", user.getUsername());

        user.setPasswordHash("newHash");
        assertEquals("newHash", user.getPasswordHash());

        user.setRole("USER");
        assertEquals("USER", user.getRole());

        user.setEnabled(false);
        assertFalse(user.isEnabled());
    }

    @Test
    void id_nullBeforePersist() {
        var user = new AppUser("alice", "hash", "ADMIN");
        assertNull(user.getId());
    }

    @Test
    void timestamps_nullBeforePersist() {
        var user = new AppUser("alice", "hash", "ADMIN");
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
    }
}
