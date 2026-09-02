package report.butt.mediamanager.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for the token-propagating wrapper. Every async view action is submitted through it, so a delegation bug
 * here breaks the UI. The token linking itself is a no-op with no agent attached and can only be observed in a real
 * trace; what's asserted here is that wrapping doesn't change executor behaviour.
 */
@NullMarked
class NewRelicTokenExecutorServiceTest {

    private final ExecutorService delegate = Executors.newFixedThreadPool(2);
    private final NewRelicTokenExecutorService executor = new NewRelicTokenExecutorService(delegate);

    @AfterEach
    void tearDown() {
        delegate.shutdownNow();
    }

    @Test
    void executeRunsTheTaskOnAWorkerThread() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var thread = new AtomicReference<String>();

        executor.execute(() -> {
            thread.set(Thread.currentThread().getName());
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "task did not run");
        assertFalse(Thread.currentThread().getName().equals(thread.get()), "task ran on the calling thread");
    }

    @Test
    void submitReturnsTheCallableResult() throws Exception {
        assertEquals("done", executor.submit(() -> "done").get(5, TimeUnit.SECONDS));
    }

    @Test
    void submitReturnsTheProvidedResultForARunnable() throws Exception {
        assertEquals("fixed", executor.submit(() -> {}, "fixed").get(5, TimeUnit.SECONDS));
    }

    @Test
    void submitPropagatesAFailure() {
        var failure = executor.submit(() -> {
            throw new IllegalStateException("boom");
        });

        var thrown = assertThrows(ExecutionException.class, () -> failure.get(5, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertEquals("boom", thrown.getCause().getMessage());
    }

    @Test
    void invokeAllRunsEveryTask() throws InterruptedException, ExecutionException {
        var results = executor.invokeAll(List.<Callable<Integer>>of(() -> 1, () -> 2, () -> 3));

        assertEquals(3, results.size());
        assertEquals(
                6,
                results.get(0).get() + results.get(1).get() + results.get(2).get());
    }

    @Test
    void invokeAnyReturnsOneResult() throws InterruptedException, ExecutionException {
        var result = executor.invokeAny(List.<Callable<Integer>>of(() -> 7));

        assertEquals(7, result);
    }

    @Test
    void completableFutureSuppliesThroughTheWrapper() {
        // The shape every view uses: supplyAsync(..., uiTaskExecutor) reaches the wrapper via execute().
        assertEquals("value", CompletableFuture.supplyAsync(() -> "value", executor).join());
    }

    @Test
    void lifecycleDelegates() throws InterruptedException {
        assertFalse(executor.isShutdown());
        assertFalse(executor.isTerminated());

        executor.shutdown();

        assertTrue(executor.isShutdown());
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(executor.isTerminated());
        assertTrue(delegate.isShutdown(), "shutdown did not reach the delegate");
    }
}
