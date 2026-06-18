package report.butt.mediamanager.route;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import report.butt.mediamanager.security.AppUser;
import report.butt.mediamanager.security.Role;
import report.butt.mediamanager.security.SecurityUtils;
import report.butt.mediamanager.security.UserAdminService;

/** Admin-only user management: list users and add, reset-password, enable/disable, or delete them. */
@Route("users")
@PageTitle("Users")
@RolesAllowed("ADMIN")
public class UserAdminView extends VerticalLayout {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private final UserAdminService userAdminService;
    private final Grid<AppUser> grid = new Grid<>(AppUser.class, false);

    public UserAdminView(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
        setWidthFull();

        add(new H2("Users"));

        grid.addColumn(AppUser::getUsername)
                .setHeader("Username")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(AppUser::getRole).setHeader("Role").setAutoWidth(true);
        grid.addColumn(u -> u.isEnabled() ? "Yes" : "No").setHeader("Enabled").setAutoWidth(true);
        grid.addColumn(u -> u.getCreatedAt() == null ? "—" : DATE.format(u.getCreatedAt()))
                .setHeader("Created")
                .setAutoWidth(true);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        GridContextMenu<AppUser> menu = grid.addContextMenu();
        menu.addItem("Reset Password", e -> e.getItem().ifPresent(this::openResetPasswordDialog));
        GridMenuItem<AppUser> enableDisable =
                menu.addItem("Disable", e -> e.getItem().ifPresent(this::toggleEnabled));
        menu.addItem("Delete", e -> e.getItem().ifPresent(this::deleteUser));
        menu.setDynamicContentHandler(user -> {
            if (user == null) {
                return false;
            }
            enableDisable.setText(user.isEnabled() ? "Disable" : "Enable");
            return true;
        });

        add(new Button("Add user", e -> openAddUserDialog()), grid);
        refresh();
    }

    private void refresh() {
        grid.setItems(userAdminService.list());
    }

    private void openAddUserDialog() {
        var dialog = new Dialog();
        dialog.setHeaderTitle("Add user");

        var username = new TextField("Username");
        username.setWidthFull();
        var password = new PasswordField("Password");
        password.setWidthFull();
        Select<Role> role = new Select<>();
        role.setLabel("Role");
        role.setItems(Role.values());
        role.setValue(Role.USER);

        var submit = new Button("Create", e -> {
            try {
                userAdminService.create(username.getValue(), password.getValue(), role.getValue());
                dialog.close();
                refresh();
                Notification.show("Created user '" + username.getValue().trim() + "'.");
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage());
            }
        });
        var cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(username, password, role);
        dialog.getFooter().add(cancel, submit);
        dialog.open();
    }

    private void openResetPasswordDialog(AppUser user) {
        var dialog = new Dialog();
        dialog.setHeaderTitle("Reset password for \"" + user.getUsername() + "\"");

        var password = new PasswordField("New password");
        password.setWidthFull();

        var submit = new Button("Reset", e -> {
            try {
                userAdminService.resetPassword(user.getId(), password.getValue());
                dialog.close();
                Notification.show("Password reset for '" + user.getUsername() + "'.");
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage());
            }
        });
        var cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(password);
        dialog.getFooter().add(cancel, submit);
        dialog.open();
    }

    private void toggleEnabled(AppUser user) {
        try {
            userAdminService.setEnabled(user.getId(), !user.isEnabled(), SecurityUtils.currentUsername());
            refresh();
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void deleteUser(AppUser user) {
        try {
            userAdminService.delete(user.getId(), SecurityUtils.currentUsername());
            refresh();
            Notification.show("Deleted user '" + user.getUsername() + "'.");
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
        }
    }
}
