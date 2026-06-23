package report.butt.mediamanager.route;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import report.butt.mediamanager.security.SecurityUtils;

/**
 * Application shell applied automatically to every authenticated view (via {@link Layout}). Provides the drawer
 * navigation (Movies/TV, plus Admin/Users for admins) and a logout action in the navbar. {@code LoginView} opts out
 * with {@code autoLayout = false}.
 */
@Layout
@PermitAll
@NullMarked
public class MainLayout extends AppLayout {

    public MainLayout(AuthenticationContext authenticationContext, ObjectProvider<BuildProperties> buildProperties) {
        var title = new H1("Media Manager");
        title.getStyle().set("font-size", "var(--aura-font-size-l)").set("margin", "0");

        var navbar = new HorizontalLayout(
                new DrawerToggle(), title, new Button("Logout", e -> authenticationContext.logout()));
        navbar.setWidthFull();
        navbar.setPadding(true);
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.expand(title);
        addToNavbar(navbar);

        var nav = new SideNav();
        nav.addItem(new SideNavItem("Movies", MovieRequestView.class));
        nav.addItem(new SideNavItem("TV", TvRequestView.class));
        if (SecurityUtils.isAdmin()) {
            var separator = new Hr();
            separator.getStyle().set("margin", "var(--vaadin-padding-s) 0");
            nav.getElement().appendChild(separator.getElement());
            nav.addItem(new SideNavItem("Notifications", NotificationsView.class));
            nav.addItem(new SideNavItem("Stats", StatsView.class));
            nav.addItem(new SideNavItem("Users", UserAdminView.class));
        }
        addToDrawer(nav);

        BuildProperties build = buildProperties.getIfAvailable();
        var version = new Span("v" + (build != null ? build.getVersion() : "dev"));
        version.getStyle().set("font-size", "var(--aura-font-size-xs)");

        var footer = new Div(version);
        footer.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("color", "var(--vaadin-text-color-secondary)")
                .set("margin-top", "auto")
                .set("padding", "var(--vaadin-padding-s)");

        if (build != null) {
            String detail = buildDetail(build);
            if (!detail.isEmpty()) {
                var meta = new Span(detail);
                meta.getStyle().set("font-size", "var(--aura-font-size-xs)").set("opacity", "0.7");
                footer.add(meta);
            }
        }
        addToDrawer(footer);
    }

    private static final DateTimeFormatter BUILD_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    /** Renders the build date and, when available, the short git sha as "{@code 2026-06-14 · a10eba5}". */
    private static String buildDetail(BuildProperties build) {
        var sb = new StringBuilder();
        if (build.getTime() != null) {
            sb.append(BUILD_DATE.format(build.getTime()));
        }
        String commit = build.get("commit");
        if (commit != null && !commit.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(commit);
        }
        return sb.toString();
    }
}
