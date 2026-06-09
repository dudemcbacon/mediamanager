package report.butt.mediamanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserAdminServiceTest {

    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserAdminService service = new UserAdminService(repository, passwordEncoder);

    @Test
    void create_encodesPasswordAndSavesWithRole() {
        when(repository.findByUsername("bob")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("ENC");

        service.create("bob", "secret", Role.USER);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(repository).save(saved.capture());
        assertEquals("bob", saved.getValue().getUsername());
        assertEquals("ENC", saved.getValue().getPasswordHash());
        assertEquals("USER", saved.getValue().getRole());
    }

    @Test
    void create_trimsUsername() {
        when(repository.findByUsername("bob")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("ENC");

        service.create("  bob  ", "secret", Role.ADMIN);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(repository).save(saved.capture());
        assertEquals("bob", saved.getValue().getUsername());
    }

    @Test
    void create_rejectsBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> service.create("  ", "secret", Role.USER));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void create_rejectsBlankPassword() {
        assertThrows(IllegalArgumentException.class, () -> service.create("bob", " ", Role.USER));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void create_rejectsDuplicateUsername() {
        when(repository.findByUsername("bob")).thenReturn(Optional.of(new AppUser("bob", "x", "USER")));
        assertThrows(IllegalArgumentException.class, () -> service.create("bob", "secret", Role.USER));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resetPassword_encodesAndSaves() {
        AppUser user = new AppUser("bob", "old", "USER");
        when(repository.findById(anyLong())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new")).thenReturn("ENC");

        service.resetPassword(1L, "new");

        assertEquals("ENC", user.getPasswordHash());
        verify(repository).save(user);
    }

    @Test
    void resetPassword_rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(1L, " "));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void delete_rejectsSelf() {
        AppUser me = new AppUser("admin", "x", "ADMIN");
        when(repository.findById(anyLong())).thenReturn(Optional.of(me));
        assertThrows(IllegalArgumentException.class, () -> service.delete(1L, "admin"));
        verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void delete_rejectsLastEnabledAdmin() {
        AppUser onlyAdmin = new AppUser("admin", "x", "ADMIN");
        when(repository.findById(anyLong())).thenReturn(Optional.of(onlyAdmin));
        when(repository.findAll()).thenReturn(List.of(onlyAdmin, new AppUser("bob", "x", "USER")));
        assertThrows(IllegalArgumentException.class, () -> service.delete(1L, "someone-else"));
        verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void delete_allowsNonLastAdmin() {
        AppUser target = new AppUser("bob", "x", "USER");
        when(repository.findById(anyLong())).thenReturn(Optional.of(target));
        service.delete(1L, "admin");
        verify(repository).delete(target);
    }

    @Test
    void setEnabled_rejectsDisablingSelf() {
        AppUser me = new AppUser("admin", "x", "ADMIN");
        when(repository.findById(anyLong())).thenReturn(Optional.of(me));
        assertThrows(IllegalArgumentException.class, () -> service.setEnabled(1L, false, "admin"));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void setEnabled_rejectsDisablingLastEnabledAdmin() {
        AppUser onlyAdmin = new AppUser("admin", "x", "ADMIN");
        when(repository.findById(anyLong())).thenReturn(Optional.of(onlyAdmin));
        when(repository.findAll()).thenReturn(List.of(onlyAdmin));
        assertThrows(IllegalArgumentException.class, () -> service.setEnabled(1L, false, "other"));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void setEnabled_allowsEnabling() {
        AppUser user = new AppUser("bob", "x", "USER");
        user.setEnabled(false);
        when(repository.findById(anyLong())).thenReturn(Optional.of(user));
        service.setEnabled(1L, true, "admin");
        verify(repository).save(user);
    }

    @Test
    void setEnabled_allowsDisablingNonLastAdmin() {
        AppUser target = new AppUser("admin2", "x", "ADMIN");
        when(repository.findById(anyLong())).thenReturn(Optional.of(target));
        when(repository.findAll()).thenReturn(List.of(new AppUser("admin1", "x", "ADMIN"), target));
        service.setEnabled(1L, false, "admin1");
        verify(repository).save(target);
    }
}
