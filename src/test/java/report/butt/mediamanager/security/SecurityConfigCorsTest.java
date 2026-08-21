package report.butt.mediamanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Pins the CORS policy's origin matching. The point of these tests is the negative cases: a subdomain wildcard is easy
 * to write in a way that also accepts a lookalike domain like {@code butt.report.evil.com}, and nothing else in the
 * build would catch that.
 */
@NullMarked
class SecurityConfigCorsTest {

    private static CorsConfiguration configurationForPath(String path) {
        var source = (UrlBasedCorsConfigurationSource) new SecurityConfig().corsConfigurationSource();
        var request = new MockHttpServletRequest("POST", path);
        return source.getCorsConfiguration(request);
    }

    private static CorsConfiguration apiConfiguration() {
        CorsConfiguration configuration = configurationForPath("/api/tdarr/transcode-complete");
        assertNotNull(configuration, "the API paths should have a CORS policy");
        return configuration;
    }

    // --- allowed ---

    @Test
    void allowsAButtReportSubdomain() {
        assertEquals(
                "https://mediamanager.butt.report", apiConfiguration().checkOrigin("https://mediamanager.butt.report"));
    }

    /** {@code *.butt.report} does not cover the apex, hence the second pattern. */
    @Test
    void allowsTheApexDomain() {
        assertEquals("https://butt.report", apiConfiguration().checkOrigin("https://butt.report"));
    }

    @Test
    void allowsANestedSubdomain() {
        assertEquals("https://a.b.butt.report", apiConfiguration().checkOrigin("https://a.b.butt.report"));
    }

    // --- rejected ---

    @Test
    void rejectsAnUnrelatedOrigin() {
        assertNull(apiConfiguration().checkOrigin("https://evil.com"));
    }

    /** The dangerous lookalike: butt.report appearing as a prefix of somebody else's domain. */
    @Test
    void rejectsButtReportAsASubdomainOfAnotherDomain() {
        assertNull(apiConfiguration().checkOrigin("https://butt.report.evil.com"));
    }

    /** The other lookalike: no dot separator, so the wildcard must not treat it as a subdomain. */
    @Test
    void rejectsDomainsThatMerelyEndInButtReport() {
        assertNull(apiConfiguration().checkOrigin("https://notbutt.report"));
        assertNull(apiConfiguration().checkOrigin("https://evil-butt.report"));
    }

    @Test
    void rejectsPlainHttp() {
        assertNull(apiConfiguration().checkOrigin("http://mediamanager.butt.report"));
    }

    /**
     * Documents a real limitation: the patterns carry no port, so only the default 443 matches. Calling the API from a
     * butt.report origin served on another port needs {@code https://*.butt.report:[*]} added to the patterns.
     */
    @Test
    void rejectsANonDefaultPort() {
        assertNull(apiConfiguration().checkOrigin("https://mediamanager.butt.report:8443"));
    }

    // --- scope and credentials ---

    @Test
    void appliesToApiPathsOnly() {
        assertNotNull(configurationForPath("/api/tdarr/transcode-complete"));
        assertNull(configurationForPath("/"), "Vaadin views should have no CORS policy");
        assertNull(configurationForPath("/actuator/health"), "actuator should have no CORS policy");
        assertNull(configurationForPath("/plex-cache/movie-123.json"), "plex-cache should have no CORS policy");
    }

    /** Credentials stay off so a butt.report subdomain can't act as the signed-in admin. */
    @Test
    void doesNotAllowCredentials() {
        assertEquals(false, apiConfiguration().getAllowCredentials());
    }

    @Test
    void allowsThePreflightMethodAndArbitraryRequestHeaders() {
        CorsConfiguration configuration = apiConfiguration();
        assertNotNull(configuration.checkHttpMethod(HttpMethod.POST));
        assertNotNull(configuration.checkHttpMethod(HttpMethod.OPTIONS));
        assertNotNull(configuration.checkHeaders(List.of("X-Tdarr-Token", "Content-Type")));
    }
}
