package report.butt.mediamanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class RoleTest {

    @Test
    void values_containsTwoEntries() {
        assertEquals(2, Role.values().length);
    }

    @Test
    void valueOf_admin() {
        assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
    }

    @Test
    void valueOf_user() {
        assertEquals(Role.USER, Role.valueOf("USER"));
    }
}
