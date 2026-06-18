package report.butt.mediamanager.exceptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RequestNotFoundExceptionTest {

    @Test
    void message_containsId() {
        var ex = new RequestNotFoundException(42L);
        assertTrue(ex.getMessage().contains("42"));
    }

    @Test
    void message_containsId_largeValue() {
        var ex = new RequestNotFoundException(99999L);
        assertTrue(ex.getMessage().contains("99999"));
    }
}
