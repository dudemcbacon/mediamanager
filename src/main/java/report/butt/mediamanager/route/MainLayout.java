package report.butt.mediamanager.route;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import report.butt.mediamanager.security.SecurityUtils;

/**
 * Application shell applied automatically to every authenticated view (via {@link Layout}). Provides the drawer
 * navigation (Movies/TV, plus Admin/Users for admins) and a logout action in the navbar. {@code LoginView} opts out
 * with {@code autoLayout = false}.
 */
@Layout
@PermitAll
public class MainLayout extends AppLayout {

    public MainLayout(AuthenticationContext authenticationContext) {
        H1 title = new H1("Media Manager");
        title.getStyle().set("font-size", "var(--aura-font-size-l)").set("margin", "0");

        HorizontalLayout navbar = new HorizontalLayout(
                new DrawerToggle(), title, new Button("Logout", e -> authenticationContext.logout()));
        navbar.setWidthFull();
        navbar.setPadding(true);
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.expand(title);
        addToNavbar(navbar);

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Movies", MovieRequestView.class));
        nav.addItem(new SideNavItem("TV", TvRequestView.class));
        if (SecurityUtils.isAdmin()) {
            nav.addItem(new SideNavItem("Admin", AdminView.class));
            nav.addItem(new SideNavItem("Users", UserAdminView.class));
        }
        addToDrawer(nav);
    }
}
