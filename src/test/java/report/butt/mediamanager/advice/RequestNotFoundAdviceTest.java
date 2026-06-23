package report.butt.mediamanager.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import report.butt.mediamanager.exceptions.RequestNotFoundException;

@NullMarked
class RequestNotFoundAdviceTest {

    private final RequestNotFoundAdvice advice = new RequestNotFoundAdvice();

    @Test
    void handler_returnsExceptionMessage() {
        var ex = new RequestNotFoundException(123L);
        String result = advice.requestNotFoundHandler(ex);
        assertEquals(ex.getMessage(), result);
    }

    @Test
    void handler_messageContainsId() {
        var ex = new RequestNotFoundException(55L);
        String result = advice.requestNotFoundHandler(ex);
        assertTrue(result.contains("55"));
    }
}
