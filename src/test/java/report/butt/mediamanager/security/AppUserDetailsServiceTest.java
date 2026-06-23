package report.butt.mediamanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@NullMarked
class AppUserDetailsServiceTest {

    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final AppUserDetailsService service = new AppUserDetailsService(repository);

    @Test
    void loadUserByUsername_found_returnsUserDetails() {
        var user = new AppUser("alice", "hashedPwd", "ADMIN");
        when(repository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");

        assertEquals("alice", details.getUsername());
        assertEquals("hashedPwd", details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN")));
        assertTrue(details.isEnabled());
    }

    @Test
    void loadUserByUsername_found_disabledUser_returnsDisabledDetails() {
        var user = new AppUser("bob", "pwd", "USER");
        user.setEnabled(false);
        when(repository.findByUsername("bob")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("bob");

        assertFalse(details.isEnabled());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_USER")));
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(repository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("unknown"));
    }
}
