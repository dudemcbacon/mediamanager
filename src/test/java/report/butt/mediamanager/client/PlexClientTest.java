package report.butt.mediamanager.client;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import report.butt.mediamanager.model.plex.EpisodeKey;
import report.butt.mediamanager.model.plex.PlexDirectory;
import report.butt.mediamanager.model.plex.PlexEpisodeData;
import report.butt.mediamanager.model.plex.PlexGuid;
import report.butt.mediamanager.model.plex.PlexMedia;
import report.butt.mediamanager.model.plex.PlexMediaContainer;
import report.butt.mediamanager.model.plex.PlexMetadata;
import report.butt.mediamanager.model.plex.PlexPart;
import report.butt.mediamanager.model.plex.PlexSearchResponse;
import report.butt.mediamanager.service.PlexCacheService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class PlexClientTest {

    private static final String BASE = "http://plex";
    private static final String TOKEN = "tok";

    private MockRestServiceServer server;
    private PlexCacheService cacheService;
    private ObjectMapper mapper;
    private PlexClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        cacheService = mock(PlexCacheService.class);
        mapper = JsonMapper.builder().build();
        when(cacheService.store(anyString(), anyString())).thenReturn("/plex-cache/x.json");
        client = new PlexClient(builder, mapper, cacheService, BASE, TOKEN, "TV Shows", "Movies");
    }

    // --- cacheMachineIdentifier ---

    @Test
    void cachesMachineIdentifierOnSuccess() {
        expectGet("/", json(machineResponse("MACHINE-123")));

        client.cacheMachineIdentifier();
        server.verify();

        assertEquals("MACHINE-123", client.getMachineIdentifier());
    }

    @Test
    void cacheMachineIdentifierThrowsOnEmptyResponse() {
        expectGet("/", "{}");
        assertThrows(IllegalStateException.class, () -> client.cacheMachineIdentifier());
    }

    @Test
    void cacheMachineIdentifierThrowsWhenUrlBlank() {
        var blank = new PlexClient(RestClient.builder(), mapper, cacheService, "", TOKEN, "TV Shows", "Movies");
        assertThrows(IllegalStateException.class, blank::cacheMachineIdentifier);
    }

    // --- cacheTvSectionId / cacheMoviesSectionId ---

    @Test
    void cachesTvSectionIdWhenSectionMatches() {
        expectGet("/library/sections", json(sectionsResponse(directory("TV Shows", "2"), directory("Movies", "1"))));

        client.cacheTvSectionId();
        server.verify();

        assertEquals("2", client.getTvSectionId());
    }

    @Test
    void cacheTvSectionIdThrowsWhenNoSectionMatches() {
        expectGet("/library/sections", json(sectionsResponse(directory("Other", "9"))));
        assertThrows(IllegalStateException.class, () -> client.cacheTvSectionId());
    }

    @Test
    void cacheTvSectionIdThrowsOnEmptyResponse() {
        expectGet("/library/sections", "{}");
        assertThrows(IllegalStateException.class, () -> client.cacheTvSectionId());
    }

    @Test
    void cacheTvSectionIdThrowsWhenUrlBlank() {
        var blank = new PlexClient(RestClient.builder(), mapper, cacheService, "", TOKEN, "TV Shows", "Movies");
        assertThrows(IllegalStateException.class, blank::cacheTvSectionId);
    }

    @Test
    void cachesMoviesSectionIdWhenSectionMatches() {
        expectGet("/library/sections", json(sectionsResponse(directory("Movies", "1"))));

        client.cacheMoviesSectionId();
        server.verify();

        assertEquals("1", client.getMoviesSectionId());
    }

    @Test
    void cacheMoviesSectionIdThrowsWhenNoSectionMatches() {
        expectGet("/library/sections", json(sectionsResponse(directory("TV Shows", "2"))));
        assertThrows(IllegalStateException.class, () -> client.cacheMoviesSectionId());
    }

    @Test
    void cacheMoviesSectionIdThrowsWhenUrlBlank() {
        var blank = new PlexClient(RestClient.builder(), mapper, cacheService, "", TOKEN, "TV Shows", "Movies");
        assertThrows(IllegalStateException.class, blank::cacheMoviesSectionId);
    }

    // --- query-url builders ---

    @Test
    void exposesConfigGetters() {
        assertEquals(BASE, client.getPlexUrl());
        assertEquals("TV Shows", client.getPlexTvSectionName());
    }

    @Test
    void movieQueryUrlHasTitleAndNoToken() {
        String url = client.movieQueryUrl("Inception");
        assertTrue(url.contains("/library/all"));
        assertTrue(url.contains("title=Inception"));
        assertFalse(url.contains("X-Plex-Token"));
    }

    @Test
    void showQueryUrlNullUntilTvSectionCached() {
        assertNull(client.showQueryUrl("The Show"));

        expectTvSectionsLookup();
        client.cacheTvSectionId();
        String url = client.showQueryUrl("TheShow");
        assertTrue(url.contains("/library/sections/2/all"));
        assertTrue(url.contains("title=TheShow"));
    }

    // --- getMovieByTmdbId ---

    @Test
    void getMovieByTmdbIdMatchesOnGuid() {
        PlexMetadata other = metadata("9", "tmdb://111");
        PlexMetadata wanted = metadata("10", "tmdb://27205");
        expectGet("/library/all", json(metadataResponse(other, wanted)));

        MetadataResult result = client.getMovieByTmdbId(27205, "Inception", 2010);

        assertEquals("/plex-cache/x.json", result.url());
        assertEquals("10", result.metadata().getRatingKey());
        verify(cacheService).store(eq(PlexClient.movieCacheKey(27205)), anyString());
    }

    @Test
    void getMovieByTmdbIdFallsBackToFirstWhenNoGuidMatches() {
        PlexMetadata first = metadata("10", "tmdb://999");
        expectGet("/library/all", json(metadataResponse(first)));

        MetadataResult result = client.getMovieByTmdbId(27205, "Inception", 2010);

        assertEquals("10", result.metadata().getRatingKey());
    }

    @Test
    void getMovieByTmdbIdReturnsNullMetadataWhenEmptyResults() {
        expectGet("/library/all", json(metadataResponse()));

        MetadataResult result = client.getMovieByTmdbId(1, "Nothing", 2000);

        assertNull(result.metadata());
        assertEquals("/plex-cache/x.json", result.url());
    }

    @Test
    void getMovieByTmdbIdReturnsNullMetadataOnBlankBody() {
        expectGet("/library/all", "");

        MetadataResult result = client.getMovieByTmdbId(1, "Nothing", 2000);

        assertNull(result.metadata());
    }

    // --- getAllMoviesIndexedByTmdb ---

    @Test
    void getAllMoviesIndexedByTmdbEmptyWhenSectionNotCached() {
        assertTrue(client.getAllMoviesIndexedByTmdb().isEmpty());
    }

    @Test
    void getAllMoviesIndexedByTmdbIndexesNumericGuidsOnly() {
        PlexMetadata good = metadata("10", "tmdb://27205");
        PlexMetadata nonNumeric = metadata("11", "tmdb://abc"); // hits NumberFormatException branch
        PlexMetadata nonTmdb = metadata("12", "imdb://tt1"); // skipped (wrong prefix)
        PlexMetadata noGuids = metadata("13"); // skipped (null guids)
        expectMoviesSectionsLookup();
        expectGet("/library/sections/1/all", json(metadataResponse(good, nonNumeric, nonTmdb, noGuids)));
        client.cacheMoviesSectionId();

        Map<Integer, PlexMetadata> indexed = client.getAllMoviesIndexedByTmdb();

        assertEquals(1, indexed.size());
        assertEquals("10", indexed.get(27205).getRatingKey());
    }

    @Test
    void getAllMoviesIndexedByTmdbEmptyWhenNoResults() {
        expectMoviesSectionsLookup();
        expectGet("/library/sections/1/all", json(metadataResponse()));
        client.cacheMoviesSectionId();

        assertTrue(client.getAllMoviesIndexedByTmdb().isEmpty());
    }

    // --- cacheMovieMetadata / cacheTvMetadata ---

    @Test
    void cacheMovieMetadataStoresSerializedWrapper() {
        String url = client.cacheMovieMetadata(27205, metadata("10", "tmdb://27205"));
        assertEquals("/plex-cache/x.json", url);
        verify(cacheService).store(eq(PlexClient.movieCacheKey(27205)), anyString());
    }

    @Test
    void cacheMovieMetadataToleratesNullMetadata() {
        client.cacheMovieMetadata(27205, null);
        verify(cacheService).store(eq(PlexClient.movieCacheKey(27205)), anyString());
    }

    @Test
    void cacheTvMetadataStoresSerializedWrapper() {
        client.cacheTvMetadata(555, metadata("3", "tvdb://555"));
        verify(cacheService).store(eq(PlexClient.tvCacheKey(555)), anyString());
    }

    @Test
    void cacheTvMetadataToleratesNullMetadata() {
        client.cacheTvMetadata(555, null);
        verify(cacheService).store(eq(PlexClient.tvCacheKey(555)), anyString());
    }

    // --- getShowByTvdbId ---

    @Test
    void getShowByTvdbIdReturnsEmptyWhenTvSectionNotCached() {
        MetadataResult result = client.getShowByTvdbId(555, "Show", 2020);
        assertNull(result.url());
        assertNull(result.metadata());
    }

    @Test
    void getShowByTvdbIdMatchesOnGuid() {
        PlexMetadata wanted = metadata("3", "tvdb://555");
        expectTvSectionsLookup();
        expectGet("/library/sections/2/all", json(metadataResponse(metadata("4", "tvdb://1"), wanted)));
        client.cacheTvSectionId();

        MetadataResult result = client.getShowByTvdbId(555, "Show", 2020);

        assertEquals("3", result.metadata().getRatingKey());
    }

    @Test
    void getShowByTvdbIdReturnsNullMetadataWhenEmpty() {
        expectTvSectionsLookup();
        expectGet("/library/sections/2/all", json(metadataResponse()));
        client.cacheTvSectionId();

        MetadataResult result = client.getShowByTvdbId(555, "Show", 2020);

        assertNull(result.metadata());
    }

    // --- getShowGrandchildren ---

    @Test
    void getShowGrandchildrenReturnsMetadataList() {
        expectGet("/library/metadata/77/grandchildren", json(metadataResponse(metadata("e1"), metadata("e2"))));

        List<PlexMetadata> grandchildren = client.getShowGrandchildren("77");

        assertEquals(2, grandchildren.size());
    }

    @Test
    void getShowGrandchildrenEmptyWhenNoContainer() {
        expectGet("/library/metadata/77/grandchildren", "{}");
        assertTrue(client.getShowGrandchildren("77").isEmpty());
    }

    // --- getAllShowsIndexedByTvdb ---

    @Test
    void getAllShowsIndexedByTvdbEmptyWhenSectionNotCached() {
        assertTrue(client.getAllShowsIndexedByTvdb().isEmpty());
    }

    @Test
    void getAllShowsIndexedByTvdbIndexesNumericGuidsOnly() {
        PlexMetadata good = metadata("3", "tvdb://555");
        PlexMetadata nonNumeric = metadata("4", "tvdb://xyz");
        PlexMetadata noGuids = metadata("5");
        expectTvSectionsLookup();
        expectGet("/library/sections/2/all", json(metadataResponse(good, nonNumeric, noGuids)));
        client.cacheTvSectionId();

        Map<Integer, PlexMetadata> indexed = client.getAllShowsIndexedByTvdb();

        assertEquals(1, indexed.size());
        assertEquals("3", indexed.get(555).getRatingKey());
    }

    // --- getAllEpisodesIndexedByShow ---

    @Test
    void getAllEpisodesIndexedByShowEmptyWhenSectionNotCached() {
        assertTrue(client.getAllEpisodesIndexedByShow().isEmpty());
    }

    @Test
    void getAllEpisodesIndexedByShowBuildsPerShowEpisodeMap() {
        PlexMetadata ep1 = episode("show-1", 1, 1, "/tv/s01e01.mkv");
        PlexMetadata missingFile = episode("show-1", 1, 2, null); // skipped (no file)
        PlexMetadata missingKey = episode(null, 1, 3, "/tv/orphan.mkv"); // skipped (no show key)
        expectTvSectionsLookup();
        expectGet("/library/sections/2/all", json(metadataResponse(ep1, missingFile, missingKey)));
        client.cacheTvSectionId();

        Map<String, Map<EpisodeKey, PlexEpisodeData>> indexed = client.getAllEpisodesIndexedByShow();

        assertEquals(1, indexed.size());
        PlexEpisodeData data = indexed.get("show-1").get(new EpisodeKey(1, 1));
        assertEquals("/tv/s01e01.mkv", data.path());
        assertEquals(1234L, data.size());
    }

    // --- static cache keys ---

    @Test
    void cacheKeyHelpers() {
        assertEquals("movie-7", PlexClient.movieCacheKey(7));
        assertEquals("tv-7", PlexClient.tvCacheKey(7));
    }

    // --- helpers ---

    // MockRestServiceServer requires every expect() to be declared before any request is made, so these
    // helpers only declare the section-lookup stub; the caller invokes cacheTvSectionId()/cacheMoviesSectionId()
    // after all expectations (including the follow-up bulk request) are set up.
    private void expectTvSectionsLookup() {
        expectGet("/library/sections", json(sectionsResponse(directory("TV Shows", "2"))));
    }

    private void expectMoviesSectionsLookup() {
        expectGet("/library/sections", json(sectionsResponse(directory("Movies", "1"))));
    }

    private void expectGet(String pathFragment, String body) {
        server.expect(requestTo(containsString(pathFragment)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private String json(Object value) {
        return mapper.writeValueAsString(value);
    }

    private static PlexSearchResponse machineResponse(String machineId) {
        var container = new PlexMediaContainer();
        container.setMachineIdentifier(machineId);
        var response = new PlexSearchResponse();
        response.setMediaContainer(container);
        return response;
    }

    private static PlexSearchResponse sectionsResponse(PlexDirectory... directories) {
        var container = new PlexMediaContainer();
        container.setDirectory(List.of(directories));
        var response = new PlexSearchResponse();
        response.setMediaContainer(container);
        return response;
    }

    private static PlexSearchResponse metadataResponse(PlexMetadata... metadata) {
        var container = new PlexMediaContainer();
        container.setMetadata(List.of(metadata));
        var response = new PlexSearchResponse();
        response.setMediaContainer(container);
        return response;
    }

    private static PlexDirectory directory(String title, String key) {
        var directory = new PlexDirectory();
        directory.setTitle(title);
        directory.setKey(key);
        return directory;
    }

    private static PlexMetadata metadata(String ratingKey, String... guidIds) {
        var metadata = new PlexMetadata();
        metadata.setRatingKey(ratingKey);
        if (guidIds.length > 0) {
            List<PlexGuid> guids = new ArrayList<>();
            for (String id : guidIds) {
                var guid = new PlexGuid();
                guid.setId(id);
                guids.add(guid);
            }
            metadata.setGuids(guids);
        }
        return metadata;
    }

    private static PlexMetadata episode(
            @Nullable String showRatingKey, Integer season, Integer number, @Nullable String file) {
        var episode = new PlexMetadata();
        episode.setGrandparentRatingKey(showRatingKey);
        episode.setParentIndex(season);
        episode.setIndex(number);
        if (file != null) {
            var part = new PlexPart();
            part.setFile(file);
            part.setSize(1234L);
            var media = new PlexMedia();
            media.setPart(List.of(part));
            episode.setMedia(List.of(media));
        }
        return episode;
    }

    @Test
    void getShowGrandchildrenEmptyWhenMetadataListNull() {
        // Container present but no metadata list -> empty list (not null).
        var container = new PlexMediaContainer();
        var response = new PlexSearchResponse();
        response.setMediaContainer(container);
        expectGet("/library/metadata/5/grandchildren", json(response));

        List<PlexMetadata> grandchildren = client.getShowGrandchildren("5");

        assertNotNull(grandchildren);
        assertTrue(grandchildren.isEmpty());
    }
}
