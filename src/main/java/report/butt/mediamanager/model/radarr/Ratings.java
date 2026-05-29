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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"imdb", "tmdb", "metacritic", "rottenTomatoes", "trakt"})
@Generated("jsonschema2pojo")
public class Ratings {

    @JsonProperty("imdb")
    private Imdb imdb;

    @JsonProperty("tmdb")
    private Tmdb tmdb;

    @JsonProperty("metacritic")
    private Metacritic metacritic;

    @JsonProperty("rottenTomatoes")
    private RottenTomatoes rottenTomatoes;

    @JsonProperty("trakt")
    private Trakt trakt;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("imdb")
    public Imdb getImdb() {
        return imdb;
    }

    @JsonProperty("imdb")
    public void setImdb(Imdb imdb) {
        this.imdb = imdb;
    }

    @JsonProperty("tmdb")
    public Tmdb getTmdb() {
        return tmdb;
    }

    @JsonProperty("tmdb")
    public void setTmdb(Tmdb tmdb) {
        this.tmdb = tmdb;
    }

    @JsonProperty("metacritic")
    public Metacritic getMetacritic() {
        return metacritic;
    }

    @JsonProperty("metacritic")
    public void setMetacritic(Metacritic metacritic) {
        this.metacritic = metacritic;
    }

    @JsonProperty("rottenTomatoes")
    public RottenTomatoes getRottenTomatoes() {
        return rottenTomatoes;
    }

    @JsonProperty("rottenTomatoes")
    public void setRottenTomatoes(RottenTomatoes rottenTomatoes) {
        this.rottenTomatoes = rottenTomatoes;
    }

    @JsonProperty("trakt")
    public Trakt getTrakt() {
        return trakt;
    }

    @JsonProperty("trakt")
    public void setTrakt(Trakt trakt) {
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
