package report.butt.mediamanager.config;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

@Configuration
public class AsyncExecutorConfig {

    /**
     * Dedicated pool for the views' blocking work — remote Ombi/Radarr/Sonarr/Deluge/SABnzbd/Plex calls and the DB
     * snapshot reads they kick off via {@code CompletableFuture}. Keeping it off the common {@code ForkJoinPool} means
     * one slow, VPN-fronted integration can't starve async UI work for every other user (the common pool is sized to
     * the CPU count and shared JVM-wide). Bounded so a burst of slow calls queues rather than spawning unbounded
     * threads; Spring shuts it down on context close.
     *
     * <p>Wrapped in a {@link DelegatingSecurityContextExecutorService} so the Spring Security context is propagated to
     * the worker threads. The context is thread-local: it's present on the Vaadin UI thread that submits the task but
     * absent on a plain pool thread, so {@code @PreAuthorize}-guarded controller calls (e.g.
     * {@code MovieController.markAvailable}) would otherwise fail with
     * {@code AuthenticationCredentialsNotFoundException}. The wrapper captures the context at submit time and restores
     * the worker's previous context afterward, so it doesn't leak between pooled tasks.
     *
     * <p>The wrapper freezes the {@link org.springframework.security.core.context.SecurityContextHolderStrategy} at
     * construction time (see {@code AbstractDelegatingSecurityContextSupport}). With {@code @Push}, a context-menu
     * action can arrive over the websocket, which never passes through Spring's {@code SecurityContextHolderFilter} —
     * so the plain thread-local strategy is empty on the submitting UI thread and an empty context gets propagated.
     * Vaadin's {@link VaadinAwareSecurityContextHolderStrategy} resolves the context from the {@code VaadinSession}
     * instead, but only if the wrapper actually uses it. Injecting it (which also forces it to be created first) and
     * setting it explicitly guarantees that, regardless of bean initialization order.
     */
    @Bean(destroyMethod = "shutdown")
    ExecutorService uiTaskExecutor(VaadinAwareSecurityContextHolderStrategy securityContextHolderStrategy) {
        var factory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                var thread = new Thread(r, "ui-task-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
        var executor = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(16, factory));
        executor.setSecurityContextHolderStrategy(securityContextHolderStrategy);
        return executor;
    }
}
