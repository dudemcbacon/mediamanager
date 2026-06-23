package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"imdb", "tmdb", "metacritic", "rottenTomatoes", "trakt"})
@Generated("jsonschema2pojo")
@NullMarked
public class Ratings {

    @JsonProperty("imdb")
    private @Nullable Imdb imdb;

    @JsonProperty("tmdb")
    private @Nullable Tmdb tmdb;

    @JsonProperty("metacritic")
    private @Nullable Metacritic metacritic;

    @JsonProperty("rottenTomatoes")
    private @Nullable RottenTomatoes rottenTomatoes;

    @JsonProperty("trakt")
    private @Nullable Trakt trakt;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("imdb")
    public @Nullable Imdb getImdb() {
        return imdb;
    }

    @JsonProperty("imdb")
    public void setImdb(@Nullable Imdb imdb) {
        this.imdb = imdb;
    }

    @JsonProperty("tmdb")
    public @Nullable Tmdb getTmdb() {
        return tmdb;
    }

    @JsonProperty("tmdb")
    public void setTmdb(@Nullable Tmdb tmdb) {
        this.tmdb = tmdb;
    }

    @JsonProperty("metacritic")
    public @Nullable Metacritic getMetacritic() {
        return metacritic;
    }

    @JsonProperty("metacritic")
    public void setMetacritic(@Nullable Metacritic metacritic) {
        this.metacritic = metacritic;
    }

    @JsonProperty("rottenTomatoes")
    public @Nullable RottenTomatoes getRottenTomatoes() {
        return rottenTomatoes;
    }

    @JsonProperty("rottenTomatoes")
    public void setRottenTomatoes(@Nullable RottenTomatoes rottenTomatoes) {
        this.rottenTomatoes = rottenTomatoes;
    }

    @JsonProperty("trakt")
    public @Nullable Trakt getTrakt() {
        return trakt;
    }

    @JsonProperty("trakt")
    public void setTrakt(@Nullable Trakt trakt) {
        this.trakt = trakt;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
