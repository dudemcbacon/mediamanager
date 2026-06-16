package report.butt.mediamanager.route;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.LitRenderer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import report.butt.mediamanager.model.FfprobeScan;
import report.butt.mediamanager.model.FfprobeStream;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.model.Validation;
import report.butt.mediamanager.model.deluge.DelugeTorrent;
import report.butt.mediamanager.model.sabnzbd.SabnzbdSlot;
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
            return "var(--vaadin-text-color-disabled)";
        }
        return result ? "var(--aura-green, green)" : "var(--aura-red, red)";
    }

    /** Latest validation result for a request id + validator name, or null when none is recorded. */
    static Boolean latestResultValue(
            Map<Long, Map<String, Validation>> latestValidations, Long requestId, String validationName) {
        Map<String, Validation> byName = latestValidations.get(requestId);
        Validation v = byName == null ? null : byName.get(validationName);
        return v == null ? null : v.getResult();
    }

    /**
     * A visually-hidden server-side {@link Icon} to add to a view so the {@code @vaadin/icons} iconset module loads.
     *
     * <p>The validator-result and external-link cells render {@code <vaadin-icon>} from {@link LitRenderer} template
     * strings (the {@code validatorResultRenderer}s and {@link #linkRenderer}). In the optimized production bundle,
     * Vaadin loads and registers the {@code vaadin} iconset only when a real {@link Icon} component is rendered — a bare
     * {@code @JsModule} import is never triggered for template-string icons, so those cells stay blank in production
     * (they work in dev, where the frontend loads eagerly). This is also why TV icons appear only once a row is expanded
     * today: the detail tree grid is the only place using the Java {@code VaadinIcon} API. Adding one hidden {@code
     * Icon} per view registers the iconset up front so every {@code <vaadin-icon>} resolves on load.
     */
    static Component iconsetLoader() {
        Icon loader = VaadinIcon.CHECK.create();
        loader.getStyle().set("display", "none");
        loader.getElement().setAttribute("aria-hidden", "true");
        return loader;
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
     * Numeric download percentage (0–100) for a queued item, or {@code -1} when there's no progress to show (not
     * queued, or the torrent/slot hasn't reported yet). Drives the visual progress bar; the matching text label still
     * comes from {@link #formatProgress}/{@link #formatPercentage}.
     */
    static double progressPercentOf(String protocol, DelugeTorrent torrent, SabnzbdSlot slot) {
        if (protocol == null) {
            return -1;
        }
        if (isTorrent(protocol)) {
            return torrent == null || torrent.getProgress() == null ? -1 : clampPercent(torrent.getProgress());
        }
        if (slot == null || slot.getPercentage() == null || slot.getPercentage().isBlank()) {
            return -1;
        }
        try {
            return clampPercent(Double.parseDouble(slot.getPercentage().trim()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static double clampPercent(double value) {
        return Math.max(0, Math.min(100, value));
    }

    /**
     * Lit template for a Progress cell: an optional loading spinner, then a thin determinate bar with a percentage
     * label when {@code item.pct >= 0}, otherwise the plain {@code item.label} text (e.g. a dash). Bound properties:
     * {@code pct} (double, -1 when N/A), {@code label} (string), and — only when {@code withSpinner} — {@code loading}.
     */
    static String progressCellTemplate(boolean withSpinner) {
        String spinner = withSpinner
                ? "<span class=\"status-spinner\" role=\"status\" aria-label=\"Loading\" ?hidden=\"${!item.loading}\">"
                        + "</span>"
                : "";
        String loadGuard = withSpinner ? "item.loading || " : "";
        return spinner
                + "<span class=\"mm-progress-wrap\" ?hidden=\"${" + loadGuard + "item.pct < 0}\">"
                + "<span class=\"mm-progress\"><span class=\"mm-progress-fill\" style=\"width: ${item.pct}%\"></span>"
                + "</span><span class=\"mm-progress-label\">${item.label}</span></span>"
                + "<span ?hidden=\"${" + loadGuard + "item.pct >= 0}\">${item.label}</span>";
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
        badges.getStyle().set("gap", "0.25rem");
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
        value.getStyle().set("font-size", "1.5rem").set("font-weight", "bold");
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
        spinner.getElement().setAttribute("role", "status");
        spinner.getElement().setAttribute("aria-label", "Loading");
        value.add(spinner);
    }

    // --- stat cards (Radarr/Sonarr queue + health) ---

    // Severity tints for the queue/health stat cards (defined in styles.css, color-scheme aware).
    private static final String IMPORT_BLOCKED_COLOR = "var(--mm-severity-blocked-bg)";
    private static final String IMPORT_PENDING_COLOR = "var(--mm-severity-warning-bg)";
    private static final String HEALTH_WARNING_COLOR = "var(--mm-severity-warning-bg)";

    /**
     * Updates a queue stat card from a per-state breakdown: the headline count, a tooltip listing each tracked-download
     * state, and a severity background — red when anything is {@code importBlocked} (highest priority), yellow when
     * anything is {@code importPending}. A null {@code byState} means the queue couldn't be fetched.
     */
    static void updateQueueCard(
            Card card, Span value, Tooltip tooltip, Integer totalRecords, Map<String, Long> byState) {
        if (byState == null) {
            value.setText("—");
            tooltip.setText("Queue unavailable");
            card.getStyle().remove("background-color");
            return;
        }
        value.setText(totalRecords == null ? "—" : String.valueOf(totalRecords));
        String breakdown = byState.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
        tooltip.setText(breakdown.isEmpty() ? "No active downloads" : breakdown);
        if (byState.containsKey("importBlocked")) {
            card.getStyle().set("background-color", IMPORT_BLOCKED_COLOR);
        } else if (byState.containsKey("importPending")) {
            card.getStyle().set("background-color", IMPORT_PENDING_COLOR);
        } else {
            card.getStyle().remove("background-color");
        }
    }

    /**
     * Updates a health stat card: the issue count, a tooltip listing each issue, and a yellow background when any issue
     * is a warning. A null {@code health} means health couldn't be fetched.
     */
    static <H> void updateHealthCard(
            Card card,
            Span value,
            Tooltip tooltip,
            List<H> health,
            Function<H, String> typeFn,
            Function<H, String> messageFn) {
        if (health == null) {
            value.setText("—");
            tooltip.setText("Health unavailable");
            card.getStyle().remove("background-color");
            return;
        }
        value.setText(String.valueOf(health.size()));
        if (health.isEmpty()) {
            tooltip.setText("No health issues");
            card.getStyle().remove("background-color");
            return;
        }
        tooltip.setText(health.stream()
                .map(h -> healthIssueLine(typeFn.apply(h), messageFn.apply(h)))
                .collect(Collectors.joining("\n")));
        if (health.stream().anyMatch(h -> "warning".equalsIgnoreCase(typeFn.apply(h)))) {
            card.getStyle().set("background-color", HEALTH_WARNING_COLOR);
        } else {
            card.getStyle().remove("background-color");
        }
    }

    /**
     * Runs the notification check off the UI thread and shows its summary toast. {@code setBulkButtonsEnabled} brackets
     * the work — called with {@code false} up front (to disable the toolbar's bulk buttons) and {@code true} on
     * completion, success or failure.
     */
    static void runNotificationCheck(
            UI ui,
            Logger log,
            NotificationService notificationService,
            Executor executor,
            Consumer<Boolean> setBulkButtonsEnabled) {
        setBulkButtonsEnabled.accept(false);
        Notification working = new Notification("Running notification check…");
        working.setDuration(0);
        working.setPosition(Notification.Position.BOTTOM_START);
        working.open();
        CompletableFuture.supplyAsync(notificationService::runCheck, executor)
                .whenComplete((result, throwable) -> ui.access(() -> {
                    try {
                        working.close();
                        if (throwable != null) {
                            log.warn("Notification check failed", throwable);
                            Notification.show("Notification check failed; see the server log.");
                        } else {
                            Notification.show(notificationSummary(result));
                        }
                    } finally {
                        setBulkButtonsEnabled.accept(true);
                    }
                }));
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

    // --- async actions ---

    /**
     * Runs a blocking action (e.g. a controller call that hits Ombi/Radarr/Sonarr) off the UI thread so the browser
     * isn't frozen, showing a persistent "working" notification until it finishes. Failures are logged and surfaced as
     * a toast; {@code always} (when non-null) runs on the UI thread on completion — success or failure — e.g. to
     * refresh a grid. Requires server push (see {@code @Push}).
     */
    static void runAsync(
            UI ui, Logger log, String workingMessage, Runnable action, Runnable always, Executor executor) {
        Notification working = new Notification(workingMessage);
        working.setDuration(0);
        working.setPosition(Notification.Position.BOTTOM_START);
        working.open();
        CompletableFuture.runAsync(action, executor)
                .whenComplete((unused, throwable) -> ui.access(() -> {
                    try {
                        working.close();
                        if (throwable != null) {
                            log.warn("{} failed", workingMessage, throwable);
                            Notification.show("Action failed; see the server log.");
                        }
                    } finally {
                        if (always != null) {
                            always.run();
                        }
                    }
                }));
    }

    // --- shared dialogs (used by MovieRequestView and TvRequestView) ---

    /** A read-only single-field dialog, e.g. for showing a Plex query URL. */
    static void openTextDialog(String headerTitle, String content) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(headerTitle);
        dialog.setWidth("600px");
        TextArea field = new TextArea();
        field.setReadOnly(true);
        field.setWidthFull();
        field.setValue(content);
        dialog.add(field);
        dialog.getFooter().add(new Button("Close", e -> dialog.close()));
        dialog.open();
    }

    /** Lists a request's notes (caller supplies them newest-first) in a dialog. */
    static void openNotesDialog(String requestTitle, List<Note> notes) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Notes for \"" + requestTitle + "\"");
        dialog.setWidth("600px");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(true);
        if (notes.isEmpty()) {
            body.add(new Span("No notes yet."));
        } else {
            for (Note n : notes) {
                VerticalLayout entry = new VerticalLayout();
                entry.setPadding(false);
                entry.setSpacing(false);
                Span timestamp = new Span(String.valueOf(n.getCreatedAt()));
                timestamp
                        .getStyle()
                        .set("font-size", "var(--aura-font-size-s)")
                        .set("color", "var(--vaadin-text-color-secondary)");
                Span text = new Span(n.getNotes());
                text.getStyle().set("white-space", "pre-wrap");
                entry.add(timestamp, text);
                body.add(entry);
            }
        }
        dialog.add(body);
        dialog.getFooter().add(new Button("Close", e -> dialog.close()));
        dialog.open();
    }

    /**
     * A free-text entry dialog with optional canned-value preset buttons. On submit it passes the entered text to
     * {@code onSubmit} and closes; when {@code requireNonBlank} is set, a blank value keeps the dialog open instead.
     */
    static void openTextEntryDialog(
            String headerTitle,
            String fieldLabel,
            String prefill,
            List<String> cannedValues,
            boolean requireNonBlank,
            Consumer<String> onSubmit) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(headerTitle);

        TextArea field = new TextArea(fieldLabel);
        field.setWidthFull();
        field.setMinHeight("8em");
        if (prefill != null) {
            field.setValue(prefill);
        }

        Button submit = new Button("Submit", e -> {
            String value = field.getValue();
            if (requireNonBlank && (value == null || value.isBlank())) {
                return;
            }
            onSubmit.accept(value);
            dialog.close();
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        if (cannedValues.isEmpty()) {
            dialog.add(field);
        } else {
            HorizontalLayout canned = new HorizontalLayout();
            canned.getStyle().set("flex-wrap", "wrap");
            for (String value : cannedValues) {
                canned.add(new Button(value, e -> field.setValue(value)));
            }
            dialog.add(canned, field);
        }
        dialog.getFooter().add(cancel, submit);
        dialog.open();
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
        return fieldDump(type, instance, priorityFields, Set.of());
    }

    /**
     * As {@link #fieldDump(Class, Object, List)}, but skips fields named in {@code excludedFields}. Use this to omit
     * lazy JPA associations (e.g. a child entity's back-reference to its parent), which would otherwise be navigated by
     * {@code String.valueOf(...)} and fail on a detached entity.
     */
    static FormLayout fieldDump(
            Class<?> type, Object instance, List<String> priorityFields, Set<String> excludedFields) {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("900px", 3));

        List<Field> fields = collectFields(type).stream()
                .filter(f -> !Modifier.isStatic(f.getModifiers())
                        && !f.isSynthetic()
                        && !excludedFields.contains(f.getName()))
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

    // --- ffprobe results (shared by MovieRequestView and the TV episode tree grid) ---

    /**
     * Shows an ffprobe scan's container {@code format} summary and a row per stream in a dialog. A null scan (the
     * request has never been scanned) renders a hint instead. Callers load the scan with its streams eagerly fetched.
     */
    static void openFfprobeResultsDialog(String title, FfprobeScan scan) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);
        dialog.setWidth("900px");
        if (scan == null) {
            dialog.add(new Span("No ffprobe scans yet — run \"Scan with FFprobe\" first."));
        } else {
            VerticalLayout body = new VerticalLayout(ffprobeSummary(scan), ffprobeStreamGrid(scan.getStreams()));
            body.setPadding(false);
            body.setSpacing(true);
            dialog.add(body);
        }
        dialog.getFooter().add(new Button("Close", e -> dialog.close()));
        dialog.open();
    }

    /** Container-level summary of a scan's {@code format} data. */
    private static VerticalLayout ffprobeSummary(FfprobeScan scan) {
        VerticalLayout summary = new VerticalLayout();
        summary.setPadding(false);
        summary.setSpacing(false);
        String format = scan.getFormatLongName() != null ? scan.getFormatLongName() : scan.getFormatName();
        summary.add(
                ffprobeSummaryLine("Format", format),
                ffprobeSummaryLine("Duration", formatDuration(scan.getDuration())),
                ffprobeSummaryLine("Size", formatBytes(scan.getSize())),
                ffprobeSummaryLine("Overall bit rate", formatBitRate(scan.getBitRate())),
                ffprobeSummaryLine("Streams", scan.getNbStreams() == null ? null : scan.getNbStreams().toString()),
                ffprobeSummaryLine("Scanned at", scan.getCreatedAt() == null ? null : scan.getCreatedAt().toString()));
        return summary;
    }

    private static Span ffprobeSummaryLine(String label, String value) {
        Span line = new Span();
        Span key = new Span(label + ": ");
        key.getStyle().set("color", "var(--vaadin-text-color-secondary)");
        line.add(key, new Span(value == null || value.isBlank() ? "—" : value));
        return line;
    }

    /** Per-stream table ({@code -show_streams}): index, type, codec, a type-specific detail string, and bit rate. */
    private static Grid<FfprobeStream> ffprobeStreamGrid(List<FfprobeStream> streams) {
        Grid<FfprobeStream> grid = new Grid<>(FfprobeStream.class, false);
        grid.addColumn(FfprobeStream::getStreamIndex).setHeader("#").setAutoWidth(true);
        grid.addColumn(FfprobeStream::getCodecType).setHeader("Type").setAutoWidth(true);
        grid.addColumn(FfprobeStream::getCodecName)
                .setHeader("Codec")
                .setTooltipGenerator(FfprobeStream::getCodecLongName)
                .setAutoWidth(true);
        grid.addColumn(RequestViewSupport::ffprobeStreamDetails)
                .setHeader("Details")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(s -> formatBitRate(s.getBitRate())).setHeader("Bit rate").setAutoWidth(true);
        grid.setItems(streams);
        grid.setAllRowsVisible(true);
        return grid;
    }

    /** Resolution/frame-rate for video streams, channels/sample-rate for audio, else a dash. */
    private static String ffprobeStreamDetails(FfprobeStream s) {
        List<String> parts = new ArrayList<>();
        if (Objects.equals(s.getCodecType(), "video")) {
            if (s.getWidth() != null && s.getHeight() != null) {
                parts.add(s.getWidth() + "x" + s.getHeight());
            }
            if (s.getRFrameRate() != null) {
                parts.add(s.getRFrameRate() + " fps");
            }
            if (s.getPixFmt() != null) {
                parts.add(s.getPixFmt());
            }
        } else if (Objects.equals(s.getCodecType(), "audio")) {
            if (s.getChannelLayout() != null) {
                parts.add(s.getChannelLayout());
            } else if (s.getChannels() != null) {
                parts.add(s.getChannels() + " ch");
            }
            if (s.getSampleRate() != null) {
                parts.add(s.getSampleRate() + " Hz");
            }
        }
        return parts.isEmpty() ? "—" : String.join(", ", parts);
    }

    private static String formatDuration(Double seconds) {
        if (seconds == null) {
            return "—";
        }
        long total = Math.round(seconds);
        return String.format("%d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60);
    }

    /** Human-friendly byte size: TiB above 1024 GiB, GiB above 1 GiB, otherwise MiB. Null/non-positive → "—". */
    static String formatBytes(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "—";
        }
        double gib = bytes / (1024.0 * 1024.0 * 1024.0);
        if (gib >= 1024.0) {
            return String.format("%.2f TiB", gib / 1024.0);
        }
        if (gib >= 1.0) {
            return String.format("%.2f GiB", gib);
        }
        return String.format("%.1f MiB", bytes / (1024.0 * 1024.0));
    }

    private static String formatBitRate(Long bitsPerSecond) {
        if (bitsPerSecond == null || bitsPerSecond <= 0) {
            return "—";
        }
        double mbps = bitsPerSecond / 1_000_000.0;
        if (mbps >= 1.0) {
            return String.format("%.2f Mb/s", mbps);
        }
        return String.format("%.0f kb/s", bitsPerSecond / 1000.0);
    }

    // --- dashboard helpers (shared by StatsView and NotificationsView) ---

    /** A grid sized to its content (no inner scrollbar), full width — for stacking several grids on a dashboard. */
    static <T> Grid<T> compactGrid() {
        Grid<T> grid = new Grid<>();
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        return grid;
    }

    static ProgressBar indeterminateBar() {
        ProgressBar bar = new ProgressBar();
        bar.setIndeterminate(true);
        return bar;
    }

    /** A titled section (header with a live count + grid) whose data arrives asynchronously. */
    static final class Section<T> {

        private final String title;
        private final Span header;
        private final Grid<T> grid;

        Section(String title, Grid<T> grid) {
            this.title = title;
            this.header = coloredLabel(title, "var(--vaadin-text-color)");
            this.grid = grid;
        }

        Grid<T> grid() {
            return grid;
        }

        Component layout(Component... betweenHeaderAndGrid) {
            VerticalLayout layout = new VerticalLayout();
            layout.setPadding(false);
            layout.setSpacing(false);
            layout.setWidthFull();
            layout.add(header);
            layout.add(betweenHeaderAndGrid);
            layout.add(grid);
            return layout;
        }

        void set(List<T> items) {
            header.setText(title + " (" + items.size() + ")");
            grid.setItems(items);
        }
    }
}
