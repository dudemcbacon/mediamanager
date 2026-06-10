package report.butt.mediamanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AppUserBootstrapTest {

    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private AppUserBootstrap bootstrap(String username, String password) {
        return new AppUserBootstrap(repository, passwordEncoder, username, password);
    }

    @Test
    void run_usersExist_skipsBootstrap() throws Exception {
        when(repository.count()).thenReturn(1L);
        bootstrap("admin", "password").run();
        verify(repository, never()).save(any());
    }

    @Test
    void run_noUsers_savesAdminUser() throws Exception {
        when(repository.count()).thenReturn(0L);
        when(passwordEncoder.encode("s3cret")).thenReturn("ENCODED");

        bootstrap("admin", "s3cret").run();

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(repository).save(saved.capture());
        assertEquals("admin", saved.getValue().getUsername());
        assertEquals("ENCODED", saved.getValue().getPasswordHash());
        assertEquals("ADMIN", saved.getValue().getRole());
    }

    @Test
    void run_noUsers_blankUsername_throwsIllegalState() {
        when(repository.count()).thenReturn(0L);
        assertThrows(IllegalStateException.class, () -> bootstrap("", "s3cret").run());
        verify(repository, never()).save(any());
    }

    @Test
    void run_noUsers_blankPassword_throwsIllegalState() {
        when(repository.count()).thenReturn(0L);
        assertThrows(IllegalStateException.class, () -> bootstrap("admin", "").run());
        verify(repository, never()).save(any());
    }

    @Test
    void run_noUsers_passwordEqualsUsername_throwsIllegalState() {
        when(repository.count()).thenReturn(0L);
        assertThrows(
                IllegalStateException.class, () -> bootstrap("admin", "admin").run());
        verify(repository, never()).save(any());
    }

    @Test
    void run_noUsers_whitespaceUsername_throwsIllegalState() {
        when(repository.count()).thenReturn(0L);
        assertThrows(
                IllegalStateException.class, () -> bootstrap("   ", "s3cret").run());
        verify(repository, never()).save(any());
    }

    @Test
    void run_noUsers_whitespacePassword_throwsIllegalState() {
        when(repository.count()).thenReturn(0L);
        assertThrows(
                IllegalStateException.class, () -> bootstrap("admin", "   ").run());
        verify(repository, never()).save(any());
    }
}
