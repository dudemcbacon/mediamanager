package report.butt.mediamanager.security;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create/update/delete application users for the admin user-management view. Encapsulates password encoding and the
 * lockout guards: an admin can't disable or delete their own account, and the last enabled admin can't be removed.
 */
@Service
public class UserAdminService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> list() {
        return repository.findAll();
    }

    public void create(String username, String rawPassword, Role role) {
        String trimmed = username == null ? "" : username.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        if (repository.findByUsername(trimmed).isPresent()) {
            throw new IllegalArgumentException("A user named '" + trimmed + "' already exists.");
        }
        repository.save(new AppUser(trimmed, passwordEncoder.encode(rawPassword), role.name()));
    }

    public void resetPassword(Long id, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        AppUser user = require(id);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        repository.save(user);
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled, String actingUsername) {
        AppUser user = require(id);
        if (!enabled) {
            if (user.getUsername().equals(actingUsername)) {
                throw new IllegalArgumentException("You can't disable your own account.");
            }
            if (wouldLeaveNoEnabledAdmin(user)) {
                throw new IllegalArgumentException("Can't disable the last enabled admin.");
            }
        }
        user.setEnabled(enabled);
        repository.save(user);
    }

    @Transactional
    public void delete(Long id, String actingUsername) {
        AppUser user = require(id);
        if (user.getUsername().equals(actingUsername)) {
            throw new IllegalArgumentException("You can't delete your own account.");
        }
        if (wouldLeaveNoEnabledAdmin(user)) {
            throw new IllegalArgumentException("Can't delete the last enabled admin.");
        }
        repository.delete(user);
    }

    private AppUser require(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("No such user: " + id));
    }

    /** True when removing or disabling {@code target} would leave zero enabled admins. */
    private boolean wouldLeaveNoEnabledAdmin(AppUser target) {
        boolean targetIsEnabledAdmin = target.isEnabled() && Role.ADMIN.name().equals(target.getRole());
        if (!targetIsEnabledAdmin) {
            return false;
        }
        long enabledAdmins = repository.findAll().stream()
                .filter(u -> u.isEnabled() && Role.ADMIN.name().equals(u.getRole()))
                .count();
        return enabledAdmins <= 1;
    }
}
