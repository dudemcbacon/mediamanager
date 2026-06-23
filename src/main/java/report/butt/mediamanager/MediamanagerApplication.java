package report.butt.mediamanager;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.aura.Aura;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Push
@StyleSheet("styles.css")
@StyleSheet(Aura.STYLESHEET)
@NullMarked
public class MediamanagerApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(MediamanagerApplication.class, args);
    }
}
