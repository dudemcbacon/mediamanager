package report.butt.mediamanager.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import report.butt.mediamanager.exceptions.RequestNotFoundException;

class RequestNotFoundAdviceTest {

    private final RequestNotFoundAdvice advice = new RequestNotFoundAdvice();

    @Test
    void handler_returnsExceptionMessage() {
        RequestNotFoundException ex = new RequestNotFoundException(123L);
        String result = advice.requestNotFoundHandler(ex);
        assertEquals(ex.getMessage(), result);
    }

    @Test
    void handler_messageContainsId() {
        RequestNotFoundException ex = new RequestNotFoundException(55L);
        String result = advice.requestNotFoundHandler(ex);
        assert result.contains("55");
    }
}
