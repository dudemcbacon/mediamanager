package report.butt.mediamanager.config;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardManager;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatSessionConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> disablePersistentSessions() {
        return factory -> factory.addContextCustomizers(context -> {
            Manager manager = context.getManager();
            if (manager == null) {
                var standardManager = new StandardManager();
                standardManager.setPathname(null);
                context.setManager(standardManager);
            } else if (manager instanceof StandardManager standardManager) {
                standardManager.setPathname(null);
            }
        });
    }
}
