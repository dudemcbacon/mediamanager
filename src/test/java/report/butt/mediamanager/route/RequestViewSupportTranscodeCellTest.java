package report.butt.mediamanager.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** Covers the Transcode cell's mapping from Tdarr verdict to icon, color, and text fallback. */
@NullMarked
class RequestViewSupportTranscodeCellTest {

    private static String tag(@Nullable String verdict) {
        return RequestViewSupport.transcodeCell(verdict).getElement().getTag();
    }

    private static String color(@Nullable String verdict) {
        Component cell = RequestViewSupport.transcodeCell(verdict);
        return String.valueOf(cell.getElement().getStyle().get("color"));
    }

    private static String iconName(@Nullable String verdict) {
        Component cell = RequestViewSupport.transcodeCell(verdict);
        return String.valueOf(cell.getElement().getAttribute("icon"));
    }

    private static String title(@Nullable String verdict) {
        Component cell = RequestViewSupport.transcodeCell(verdict);
        return String.valueOf(cell.getElement().getAttribute("title"));
    }

    @Test
    void queuedIsABlueFileRefreshIcon() {
        assertEquals("vaadin-icon", tag("Queued"));
        assertEquals("vaadin:file-refresh", iconName("Queued"));
        assertTrue(color("Queued").contains("--aura-blue"), color("Queued"));
        assertEquals("Queued", title("Queued"));
    }

    @Test
    void transcodeSuccessIsAGreenCheck() {
        assertEquals("vaadin:check", iconName("Transcode success"));
        assertTrue(color("Transcode success").contains("--aura-green"), color("Transcode success"));
        assertEquals("Transcode success", title("Transcode success"));
    }

    @Test
    void notRequiredIsAlsoAGreenCheck() {
        assertEquals("vaadin:check", iconName("Not required"));
        assertTrue(color("Not required").contains("--aura-green"), color("Not required"));
    }

    @Test
    void transcodeErrorIsAYellowExclamation() {
        assertEquals("vaadin:exclamation-circle", iconName("Transcode error"));
        assertTrue(color("Transcode error").contains("--aura-yellow"), color("Transcode error"));
    }

    /** An unrecognised status must be shown verbatim rather than swallowed or mapped to a misleading icon. */
    @Test
    void anyOtherStatusIsRenderedAsItsText() {
        Component cell = RequestViewSupport.transcodeCell("Transcode cancelled");

        assertEquals("span", cell.getElement().getTag());
        assertEquals("Transcode cancelled", cell.getElement().getText());
    }

    @Test
    void surroundingWhitespaceDoesNotDefeatTheIconMapping() {
        assertEquals("vaadin:check", iconName("  Transcode success  "));
    }

    @Test
    void missingVerdictIsARedX() {
        for (String verdict : new String[] {null, "", "   "}) {
            assertEquals("vaadin-icon", tag(verdict), "verdict=" + verdict);
            assertEquals("vaadin:close", iconName(verdict), "verdict=" + verdict);
            assertTrue(color(verdict).contains("--aura-red"), "verdict=" + verdict + " color=" + color(verdict));
            assertEquals("No transcode data", title(verdict), "verdict=" + verdict);
        }
    }
}
