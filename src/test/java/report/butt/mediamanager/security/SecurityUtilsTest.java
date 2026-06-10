package report.butt.mediamanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void isAdmin_trueWhenContextHasAdminRole() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin", "pwd", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        assertTrue(SecurityUtils.isAdmin());
    }

    @Test
    void isAdmin_falseWhenContextHasUserRole() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "bob", "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        assertFalse(SecurityUtils.isAdmin());
    }

    @Test
    void isAdmin_falseWhenNoAuthentication() {
        // context is clear — no auth set
        assertFalse(SecurityUtils.isAdmin());
    }

    @Test
    void currentUsername_returnsUsernameWhenAuthenticated() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "alice", "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        assertEquals("alice", SecurityUtils.currentUsername());
    }

    @Test
    void currentUsername_returnsNullWhenNoAuthentication() {
        assertNull(SecurityUtils.currentUsername());
    }
}
