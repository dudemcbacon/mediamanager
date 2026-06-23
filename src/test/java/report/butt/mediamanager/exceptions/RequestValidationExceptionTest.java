package report.butt.mediamanager.exceptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class RequestValidationExceptionTest {

    @Test
    void message_containsId() {
        var ex = new RequestValidationException(7L);
        assertTrue(ex.getMessage().contains("7"));
    }

    @Test
    void message_containsId_zeroValue() {
        var ex = new RequestValidationException(0L);
        assertTrue(ex.getMessage().contains("0"));
    }
}
