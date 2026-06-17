package report.butt.mediamanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import report.butt.mediamanager.client.PlexClient;

@SpringBootTest(
        properties = {
            "mediamanager.bootstrap.username=test",
            // Must differ from the username: AppUserBootstrap rejects equal bootstrap creds on a clean DB (CI).
            "mediamanager.bootstrap.password=test-admin-pw",
            "mediamanager.validate-required-config=false",
            // JobRunr's background server + dashboard are runtime concerns, not part of this wiring smoke test;
            // disabling them keeps the test from starting worker threads or binding the dashboard port.
            "jobrunr.background-job-server.enabled=false",
            "jobrunr.dashboard.enabled=false"
        })
class MediamanagerApplicationTests {

    // PlexClient's @PostConstruct eagerly calls the live Plex server (to cache the machine id and section ids)
    // and throws when it's unreachable. With no Plex configured under test that aborts context startup, so mock it;
    // this stays a pure wiring smoke test. (RadarrClient/SonarrClient swallow their eager-init failures, so they
    // don't need mocking.)
    @MockitoBean
    PlexClient plexClient;

    @Test
    void contextLoads() {}
}
