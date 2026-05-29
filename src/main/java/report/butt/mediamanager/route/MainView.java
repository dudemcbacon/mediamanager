package report.butt.mediamanager.route;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;

@Route
public class MainView extends VerticalLayout {

    public MainView(MovieRequestView movieRequestView, TvRequestView tvRequestView) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        Tab moviesTab = new Tab("Movies");
        Tab tvTab = new Tab("TV");
        Tabs tabs = new Tabs(moviesTab, tvTab);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.add(movieRequestView);

        tabs.addSelectedChangeListener(e -> {
            content.removeAll();
            Tab selected = e.getSelectedTab();
            if (selected == tvTab) {
                content.add(tvRequestView);
            } else {
                content.add(movieRequestView);
            }
        });

        add(tabs, content);
        expand(content);
    }
}
