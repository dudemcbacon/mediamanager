package report.butt.mediamanager.config;

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
     * {@code MovieController.markAvailable}) would otherwise fail with {@code AuthenticationCredentialsNotFoundException}.
     * The wrapper captures the context at submit time and restores the worker's previous context afterward, so it
     * doesn't leak between pooled tasks.
     */
    @Bean(destroyMethod = "shutdown")
    ExecutorService uiTaskExecutor() {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "ui-task-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
        return new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(16, factory));
    }
}
