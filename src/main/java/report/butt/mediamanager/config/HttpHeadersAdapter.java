package report.butt.mediamanager.config;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;

/** Exposes Spring's {@link HttpHeaders} through the New Relic agent's {@link Headers} view. */
@NullMarked
record HttpHeadersAdapter(HttpHeaders headers) implements Headers {

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public @Nullable String getHeader(String name) {
        return headers.getFirst(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return headers.getOrEmpty(name);
    }

    @Override
    public void setHeader(String name, String value) {
        headers.set(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        headers.add(name, value);
    }

    @Override
    public Set<String> getHeaderNames() {
        return headers.headerNames();
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsHeader(name);
    }
}
