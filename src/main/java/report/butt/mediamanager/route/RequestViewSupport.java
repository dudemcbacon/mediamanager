package report.butt.mediamanager.route;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.data.renderer.LitRenderer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.model.deluge.DelugeTorrent;
import report.butt.mediamanager.service.NotificationService;

/**
 * Stateless rendering and formatting helpers shared by {@link MovieRequestView}, {@link TvRequestView}, and
 * {@link TvHierarchyTreeGrid}. These were previously duplicated across the views (or reached across classes via
 * {@code MovieRequestView.xxx} static calls); collecting them here keeps the two parallel Movie/TV views in sync.
 */
final class RequestViewSupport {

    private RequestViewSupport() {}

    // --- validation result icon (client-side LitRenderer values) ---

    static String resultIconName(Boolean result) {
        if (result == null) {
            return "vaadin:minus";
        }
        return result ? "vaadin:check" : "vaadin:close";
    }

    static String resultIconColor(Boolean result) {
        if (result == null) {
            return "var(--lumo-tertiary-text-color)";
        }
        return result ? "var(--lumo-success-color, green)" : "var(--lumo-error-color, red)";
    }

    /** Latest validation result for a request id + validator name, or null when none is recorded. */
    static Boolean latestResultValue(
            Map<Long, Map<String, Validation>> latestValidations, Long requestId, String validationName) {
        Map<String, Validation> byName = latestValidations.get(requestId);
        Validation v = byName == null ? null : byName.get(validationName);
        return v == null ? null : v.getResult();
    }

    // --- download status formatting ---

    static boolean isTorrent(String protocol) {
        return "torrent".equalsIgnoreCase(protocol);
    }

    static String formatProgress(Double progress) {
        return progress == null ? "—" : String.format("%.1f%%", progress);
    }

    /** SABnzbd reports percentage as a whole-number string, e.g. "42"; render it as "42%". */
    static String formatPercentage(String percentage) {
        return percentage == null || percentage.isBlank() ? "—" : percentage + "%";
    }

    /** Formats a torrent's peers as "num_peers (total_peers)/num_seeds (total_seeds)". */
    static String formatPeers(DelugeTorrent t) {
        return nz(t.getNumPeers()) + " (" + nz(t.getTotalPeers()) + ")/" + nz(t.getNumSeeds()) + " ("
                + nz(t.getTotalSeeds()) + ")";
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Protocol badges for a Status/Type cell: a solid-blue {@code torrent} badge and/or a solid-green {@code usenet}
     * badge, or a dash when nothing is queued.
     */
    static Component protocolBadges(Set<String> protocols) {
        if (protocols.isEmpty()) {
            return new Span("—");
        }
        HorizontalLayout badges = new HorizontalLayout();
        badges.setSpacing(false);
        badges.getStyle().set("gap", "var(--lumo-space-xs)");
        if (protocols.stream().anyMatch(RequestViewSupport::isTorrent)) {
            Badge torrent = new Badge("torrent");
            torrent.addThemeVariants(BadgeVariant.FILLED);
            badges.add(torrent);
        }
        if (protocols.stream().anyMatch(p -> !isTorrent(p))) {
            Badge usenet = new Badge("usenet");
            usenet.addThemeVariants(BadgeVariant.SUCCESS, BadgeVariant.FILLED);
            badges.add(usenet);
        }
        return badges;
    }

    // --- small component factories ---

    static Component headerWithTooltip(String shortName, String title, String description) {
        Span label = new Span(shortName);
        Tooltip.forComponent(label).setText(title + "\n\n" + description);
        return label;
    }

    static Card statCard(String title, Span value) {
        value.getStyle().set("font-size", "var(--lumo-font-size-xxl)").set("font-weight", "bold");
        Card card = new Card();
        card.setTitle(title);
        card.add(value);
        return card;
    }

    /**
     * Shows a loading spinner inside a stat card's value while its data is fetched async. The next {@code setText} on
     * the value (when the data arrives) replaces the spinner. Uses the same {@code status-spinner} CSS as the grid.
     */
    static void showCardLoading(Span value) {
        value.removeAll();
        Span spinner = new Span();
        spinner.setClassName("status-spinner");
        value.add(spinner);
    }

    static Span coloredLabel(String text, String color) {
        Span span = new Span(text);
        span.getStyle().set("color", color);
        return span;
    }

    /** Formats a health item as a "• [type] message" bullet line for a tooltip. */
    static String healthIssueLine(String type, String message) {
        String prefix = type == null ? "" : "[" + type + "] ";
        return "• " + prefix + (message == null ? "" : message);
    }

    /** One-line outcome for the "Test Notifications" toast: nothing found, email sent, or found-but-not-sent. */
    static String notificationSummary(NotificationService.NotificationResult result) {
        int total = result.total();
        if (total == 0) {
            return "Notification check: nothing to report.";
        }
        if (result.emailSent()) {
            return "Sent summary email: " + total + " item(s). See the email for details.";
        }
        return "Found " + total + " item(s), but no email was sent (mail not configured or send failed).";
    }

    /**
     * Client-side renderer for an external-link cell: an anchor when a URL is present, otherwise a dash. Rendered by
     * the browser (LitRenderer) so wide grids stay responsive while scrolling. Right-clicks on the anchor are handled
     * by a single delegated grid listener (see {@link #suppressGridContextMenuOnLinks}).
     */
    static <T> LitRenderer<T> linkRenderer(Function<T, String> hrefFn) {
        return LitRenderer.<T>of(
                        "<a href=\"${item.href}\" target=\"_blank\" rel=\"noopener\" ?hidden=\"${!item.href}\">"
                                + "<vaadin-icon icon=\"vaadin:external-link\"></vaadin-icon></a>"
                                + "<span ?hidden=\"${item.href}\">—</span>")
                .withProperty("href", item -> {
                    String href = hrefFn.apply(item);
                    return href == null ? "" : href;
                });
    }

    /**
     * Registers one capture-phase contextmenu listener on the grid so right-clicks landing on a link let the browser's
     * native link menu show instead of the grid's context menu. Runs once on attach — far cheaper than attaching a
     * listener to every link cell as rows render.
     */
    static void suppressGridContextMenuOnLinks(Grid<?> grid) {
        grid.getElement()
                .executeJs("this.addEventListener('contextmenu', e => { if (e.target.closest('a'))"
                        + " e.stopPropagation(); }, true);");
    }

    /**
     * Builds the read-only "field dump" {@link FormLayout} shown in a request's detail row: every non-static,
     * non-synthetic field of {@code type} (walking up the class hierarchy), ordered by {@code priorityFields} first
     * then case-insensitively by name, with humanized labels.
     */
    static FormLayout fieldDump(Class<?> type, Object instance, List<String> priorityFields) {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("900px", 3));

        List<Field> fields = collectFields(type).stream()
                .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.isSynthetic())
                .sorted(Comparator.<Field>comparingInt(f -> {
                            int idx = priorityFields.indexOf(f.getName());
                            return idx == -1 ? Integer.MAX_VALUE : idx;
                        })
                        .thenComparing(Field::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        for (Field field : fields) {
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(instance);
            } catch (IllegalAccessException e) {
                value = "<inaccessible>";
            }
            Span valueSpan = new Span(value == null ? "—" : String.valueOf(value));
            layout.addFormItem(valueSpan, humanizeFieldName(field.getName()));
        }

        return layout;
    }

    private static List<Field> collectFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    private static String humanizeFieldName(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i - 1))) {
                sb.append(' ');
            }
            sb.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        return sb.toString();
    }
}
