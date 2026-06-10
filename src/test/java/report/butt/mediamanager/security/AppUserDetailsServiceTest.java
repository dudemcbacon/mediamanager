package report.butt.mediamanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class AppUserDetailsServiceTest {

    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final AppUserDetailsService service = new AppUserDetailsService(repository);

    @Test
    void loadUserByUsername_found_returnsUserDetails() {
        AppUser user = new AppUser("alice", "hashedPwd", "ADMIN");
        when(repository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");

        assertEquals("alice", details.getUsername());
        assertEquals("hashedPwd", details.getPassword());
        assert details.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        assert details.isEnabled();
    }

    @Test
    void loadUserByUsername_found_disabledUser_returnsDisabledDetails() {
        AppUser user = new AppUser("bob", "pwd", "USER");
        user.setEnabled(false);
        when(repository.findByUsername("bob")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("bob");

        assert !details.isEnabled();
        assert details.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(repository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("unknown"));
    }
}
