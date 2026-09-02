package report.butt.mediamanager.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

@NullMarked
class DistributedTraceClientHttpRequestInterceptorTest {

    private static final byte[] BODY = new byte[0];

    // --- URI sanitising: credentials must never reach New Relic as a span attribute ---

    @Test
    void reportedUriDropsTheSabnzbdApiKeyQueryParam() {
        var uri = DistributedTraceClientHttpRequestInterceptor.withoutCredentials(
                URI.create("http://sabnzbd:8080/api?mode=queue&output=json&apikey=super-secret"));

        assertEquals(URI.create("http://sabnzbd:8080/api"), uri);
        assertFalse(uri.toString().contains("super-secret"));
    }

    @Test
    void reportedUriDropsThePlexToken() {
        var uri = DistributedTraceClientHttpRequestInterceptor.withoutCredentials(
                URI.create("http://plex:32400/library/sections?X-Plex-Token=super-secret"));

        assertEquals(URI.create("http://plex:32400/library/sections"), uri);
        assertFalse(uri.toString().contains("super-secret"));
    }

    @Test
    void reportedUriDropsUserInfo() {
        var uri = DistributedTraceClientHttpRequestInterceptor.withoutCredentials(
                URI.create("http://user:super-secret@ombi/api/v1/Request/movie"));

        assertEquals(URI.create("http://ombi/api/v1/Request/movie"), uri);
        assertFalse(uri.toString().contains("super-secret"));
    }

    @Test
    void reportedUriKeepsSchemeHostPortAndPath() {
        var uri = DistributedTraceClientHttpRequestInterceptor.withoutCredentials(
                URI.create("https://sonarr:8989/api/v3/series/42"));

        assertEquals(URI.create("https://sonarr:8989/api/v3/series/42"), uri);
    }

    // --- transparency: the interceptor must not disturb the request or the response ---

    @Test
    void passesTheResponseThroughAndLeavesExistingHeadersAlone() throws IOException {
        var headers = new HttpHeaders();
        headers.add("X-Api-Key", "keep-me");
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("http://radarr/api/v3/movie"));
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getHeaders()).thenReturn(headers);
        var expected = mock(ClientHttpResponse.class);

        var response = new DistributedTraceClientHttpRequestInterceptor()
                .intercept(request, BODY, (req, body) -> expected);

        assertSame(expected, response);
        assertEquals("keep-me", headers.getFirst("X-Api-Key"));
    }

    @Test
    void toleratesAUriWithNoHost() throws IOException {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("/api/v3/movie"));
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        var expected = mock(ClientHttpResponse.class);

        assertSame(
                expected,
                new DistributedTraceClientHttpRequestInterceptor().intercept(request, BODY, (req, body) -> expected));
    }
}
