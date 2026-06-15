package report.butt.mediamanager.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategy;
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
 * Spring Security context to its worker threads, and it must capture that context via the injected
 * {@link VaadinAwareSecurityContextHolderStrategy} — which resolves it from the {@code VaadinSession} on a websocket
 * {@code @Push} thread — rather than the plain thread-local strategy the wrapper would otherwise freeze at
 * construction time. If the executor is ever unwrapped, or stops honoring the injected strategy, these tests fail.
 */
class AsyncExecutorConfigTest {

    private final VaadinAwareSecurityContextHolderStrategy strategy = new VaadinAwareSecurityContextHolderStrategy();
    private final ExecutorService executor = new AsyncExecutorConfig().uiTaskExecutor(strategy);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        strategy.clearContext();
        SecurityContextHolder.clearContext();
    }

    @Test
    void propagatesSecurityContextFromInjectedStrategy() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "tester", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        // Authenticate only on the injected strategy and leave the global SecurityContextHolder empty: the worker must
        // still see this, proving the executor captures from the injected (Vaadin-aware) strategy, not the global one.
        strategy.getContext().setAuthentication(auth);

        Authentication seenOnWorker =
                executor.submit(() -> strategy.getContext().getAuthentication()).get();

        // The task runs on a pool thread, but must see the authentication captured from the submitting thread.
        assertSame(auth, seenOnWorker);
    }

    @Test
    void capturesContextAtSubmitTimeWithNoAuthenticatedUser() throws Exception {
        // No authentication on the submitting thread -> the worker must not inherit some other thread's context.
        strategy.clearContext();

        Authentication seenOnWorker =
                executor.submit(() -> strategy.getContext().getAuthentication()).get();

        assertNull(seenOnWorker);
    }
}
