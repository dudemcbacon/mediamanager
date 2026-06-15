package report.butt.mediamanager.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Guards the fix for {@code AuthenticationCredentialsNotFoundException} when async UI actions invoke
 * {@code @PreAuthorize}-guarded controllers (e.g. "Mark Available"). The {@code uiTaskExecutor} must propagate the
 * Spring Security context to its worker threads; if it's ever unwrapped back to a plain pool, this test fails.
 */
class AsyncExecutorConfigTest {

    private final ExecutorService executor = new AsyncExecutorConfig().uiTaskExecutor();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        SecurityContextHolder.clearContext();
    }

    @Test
    void propagatesSecurityContextToWorkerThread() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "tester", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Authentication seenOnWorker = executor
                .submit(() -> SecurityContextHolder.getContext().getAuthentication())
                .get();

        // The task runs on a pool thread, but must see the authentication captured from the submitting thread.
        assertSame(auth, seenOnWorker);
    }

    @Test
    void capturesContextAtSubmitTimeWithNoAuthenticatedUser() throws Exception {
        // No authentication on the submitting thread -> the worker must not inherit some other thread's context.
        SecurityContextHolder.clearContext();

        Authentication seenOnWorker = executor
                .submit(() -> SecurityContextHolder.getContext().getAuthentication())
                .get();

        assertNull(seenOnWorker);
    }
}
