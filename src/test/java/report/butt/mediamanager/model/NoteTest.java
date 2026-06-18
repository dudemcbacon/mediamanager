package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NoteTest {

    @Test
    void constructor_setsNotesAndRequest() {
        var req = new MovieRequest("T", 1, false, 1, "S");
        var note = new Note("Some notes here", req);

        assertEquals("Some notes here", note.getNotes());
        assertEquals(req, note.getRequest());
    }

    @Test
    void setters_roundTrip() {
        var req = new MovieRequest("T", 1, false, 1, "S");
        var note = new Note("Initial", req);

        note.setNotes("Updated notes");
        assertEquals("Updated notes", note.getNotes());

        var req2 = new MovieRequest("T2", 2, false, 2, "S");
        note.setRequest(req2);
        assertEquals(req2, note.getRequest());
    }

    @Test
    void id_nullBeforePersist() {
        var req = new MovieRequest("T", 1, false, 1, "S");
        var note = new Note("Notes", req);
        assertNull(note.getId());
    }

    @Test
    void timestamps_nullBeforePersist() {
        var req = new MovieRequest("T", 1, false, 1, "S");
        var note = new Note("Notes", req);
        assertNull(note.getCreatedAt());
        assertNull(note.getUpdatedAt());
    }
}
