package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RequestTypeTest {

    @Test
    void values_containsMovieAndTv() {
        RequestType[] values = RequestType.values();
        assertEquals(2, values.length);
    }

    @Test
    void valueOf_movie() {
        assertEquals(RequestType.MOVIE, RequestType.valueOf("MOVIE"));
    }

    @Test
    void valueOf_tv() {
        assertEquals(RequestType.TV, RequestType.valueOf("TV"));
    }
}
