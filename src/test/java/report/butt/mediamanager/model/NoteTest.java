package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NoteTest {

    @Test
    void constructor_setsNotesAndRequest() {
        MovieRequest req = new MovieRequest("T", 1, false, 1, "S");
        Note note = new Note("Some notes here", req);

        assert "Some notes here".equals(note.getNotes());
        assert note.getRequest() == req;
    }

    @Test
    void setters_roundTrip() {
        MovieRequest req = new MovieRequest("T", 1, false, 1, "S");
        Note note = new Note("Initial", req);

        note.setNotes("Updated notes");
        assert "Updated notes".equals(note.getNotes());

        MovieRequest req2 = new MovieRequest("T2", 2, false, 2, "S");
        note.setRequest(req2);
        assert note.getRequest() == req2;
    }

    @Test
    void id_nullBeforePersist() {
        MovieRequest req = new MovieRequest("T", 1, false, 1, "S");
        Note note = new Note("Notes", req);
        assertNull(note.getId());
    }

    @Test
    void timestamps_nullBeforePersist() {
        MovieRequest req = new MovieRequest("T", 1, false, 1, "S");
        Note note = new Note("Notes", req);
        assertNull(note.getCreatedAt());
        assertNull(note.getUpdatedAt());
    }
}
