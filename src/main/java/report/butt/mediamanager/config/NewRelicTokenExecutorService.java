package report.butt.mediamanager.config;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.NullMarked;

/**
 * Propagates the submitting thread's New Relic transaction onto the worker thread, so work handed to a pool stays part
 * of the trace that started it.
 *
 * <p>Without this, every {@code CompletableFuture.supplyAsync(…, uiTaskExecutor)} in the views lands on a thread with no
 * transaction: a plain {@code @Trace} method creates nothing there, the RestClient interceptor's {@code startSegment}
 * returns a no-op, and no distributed-trace headers are written — so the same Deluge or Radarr call is fully traced from
 * a scheduled sweep and invisible from the UI. Wrapping the executor fixes every submission site at once, the same way
 * {@link DistributedTraceRestClientConfig} covers every outbound HTTP call.
 *
 * <p>The agent's async API needs both halves: a {@link Token} captured on the submitting thread (which keeps the
 * transaction open past the end of the HTTP request that started it) and a {@code linkAndExpire} from inside a
 * {@code @Trace(async = true)} method on the worker. A lambda body isn't a traced method, hence the two small static
 * hand-offs below.
 *
 * <p>Off a transaction the agent hands back a no-op token and the wrapping costs nothing. A task that is submitted but
 * never runs (executor shut down before it is dequeued) leaks its token until the agent's own token timeout reaps it —
 * bounded, and only at shutdown, since the delegate pools here are unbounded-queue and never reject.
 *
 * <p>This mirrors {@link org.springframework.security.concurrent.DelegatingSecurityContextExecutorService}, which does
 * the same capture-at-submit trick for the Spring Security context; the two compose.
 */
@NullMarked
public class NewRelicTokenExecutorService implements ExecutorService {

    private final ExecutorService delegate;

    public NewRelicTokenExecutorService(ExecutorService delegate) {
        this.delegate = delegate;
    }

    // --- submission: capture the token here, on the calling thread ---

    @Override
    public void execute(Runnable command) {
        delegate.execute(linked(command));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(linked(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(linked(task), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(linked(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(linkedAll(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(linkedAll(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(linkedAll(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(linkedAll(tasks), timeout, unit);
    }

    private static Runnable linked(Runnable command) {
        var token = NewRelic.getAgent().getTransaction().getToken();
        return () -> runLinked(token, command);
    }

    private static <T> Callable<T> linked(Callable<T> task) {
        var token = NewRelic.getAgent().getTransaction().getToken();
        return () -> callLinked(token, task);
    }

    private static <T> List<Callable<T>> linkedAll(Collection<? extends Callable<T>> tasks) {
        return tasks.stream().map(NewRelicTokenExecutorService::<T>linked).toList();
    }

    // --- execution: link on the worker thread, from inside a traced method ---

    @Trace(async = true)
    private static void runLinked(Token token, Runnable command) {
        token.linkAndExpire();
        command.run();
    }

    @Trace(async = true)
    private static <T> T callLinked(Token token, Callable<T> task) throws Exception {
        token.linkAndExpire();
        return task.call();
    }

    // --- lifecycle: plain delegation ---

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
}
