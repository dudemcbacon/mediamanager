package report.butt.mediamanager.model;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * Guards hash-based change detection in the refresh services: every field mutated by the refresh
 * {@code applyUpdates} helpers must be reflected in {@code hashCode()}, otherwise a changed row
 * hashes equal to its preloaded state and the update is silently dropped from {@code saveAll}.
 *
 * <p>Each case starts from a populated entity, mutates one field to a distinct value, and asserts
 * the hash changed.
 */
class RequestHashCodeTest {

    @Test
    void tvRequestHashCoversEveryRefreshedField() {
        TvRequest tv = new TvRequest("Title", 100, false, 5000, "Common.Approved");
        tv.setId(1L);

        int h = tv.hashCode();
        h = assertChanged(h, tv, () -> tv.setTitle("New Title"));
        h = assertChanged(h, tv, () -> tv.setTvdbId(200));
        h = assertChanged(h, tv, () -> tv.setOmbiAvailable(true));
        h = assertChanged(h, tv, () -> tv.setOmbiRequestStatus("Common.Available"));
        h = assertChanged(h, tv, () -> tv.setOmbiUserName("alice"));
        h = assertChanged(h, tv, () -> tv.setOmbiExternalProviderId(42));
        h = assertChanged(h, tv, () -> tv.setOmbiTotalSeasons(3));
        h = assertChanged(h, tv, () -> tv.setSonarrSeriesId(77));
        h = assertChanged(h, tv, () -> tv.setSonarrTitleSlug("show-title-slug"));
        h = assertChanged(h, tv, () -> tv.setSonarrMonitored(true));
        h = assertChanged(h, tv, () -> tv.setSonarrMonitoredAll("all"));
        h = assertChanged(h, tv, () -> tv.setSonarrPath("/tv/show"));
        h = assertChanged(h, tv, () -> tv.setSonarrRootFolderPath("/tv"));
        h = assertChanged(h, tv, () -> tv.setSonarrOriginalLanguage("English"));
        h = assertChanged(h, tv, () -> tv.setSonarrEpisodeFileCount(10));
        h = assertChanged(h, tv, () -> tv.setSonarrEpisodeCount(12));
        h = assertChanged(h, tv, () -> tv.setSonarrTotalEpisodeCount(13));
        h = assertChanged(h, tv, () -> tv.setPlexMetadataUrl("/plex-cache/tv-200.json"));
        h = assertChanged(h, tv, () -> tv.setPlexMetadataId("9001"));
        h = assertChanged(h, tv, () -> tv.setPlexAddedAt(1000L));
        h = assertChanged(h, tv, () -> tv.setPlexUpdatedAt(2000L));
        h = assertChanged(h, tv, () -> tv.setPlexTvdbId(200));
        h = assertChanged(h, tv, () -> tv.setPlexMediaId(55));
        h = assertChanged(h, tv, () -> tv.setPlexMediaDuration(3600L));
        h = assertChanged(h, tv, () -> tv.setPlexMediaFilename("/media/show.mkv"));
        assertChanged(h, tv, () -> tv.setPlexMediaSize(123456L));
    }

    @Test
    void tvChildRequestHashCoversEveryRefreshedField() {
        TvRequest parent = new TvRequest("Parent", 100, false, 5000, "Common.Approved");
        parent.setId(1L);
        TvChildRequest child = new TvChildRequest(parent, "Child", 100, false, 6000, "Common.Approved");
        child.setId(2L);

        int h = child.hashCode();
        h = assertChanged(h, child, () -> child.setOmbiParentRequestId(5001));
        h = assertChanged(h, child, () -> child.setTitle("New Child"));
        h = assertChanged(h, child, () -> child.setOmbiAvailable(true));
        h = assertChanged(h, child, () -> child.setOmbiRequestStatus("Common.Available"));
        h = assertChanged(h, child, () -> child.setOmbiUserName("bob"));
        assertChanged(h, child, () -> child.setOmbiTotalSeasons(4));
    }

    @Test
    void tvSeasonRequestHashCoversEveryRefreshedField() {
        TvRequest parent = new TvRequest("Parent", 100, false, 5000, "Common.Approved");
        parent.setId(1L);
        TvChildRequest child = new TvChildRequest(parent, "Child", 100, false, 6000, "Common.Approved");
        child.setId(2L);
        TvSeasonRequest season = new TvSeasonRequest(child, 7000, 1, false);
        season.setId(3L);

        int h = season.hashCode();
        h = assertChanged(h, season, () -> season.setOmbiSeasonRequestId(7001));
        h = assertChanged(h, season, () -> season.setOmbiSeasonNumber(2));
        assertChanged(h, season, () -> season.setOmbiSeasonAvailable(true));
    }

    @Test
    void tvEpisodeRequestHashCoversEveryRefreshedField() {
        TvRequest parent = new TvRequest("Parent", 100, false, 5000, "Common.Approved");
        parent.setId(1L);
        TvChildRequest child = new TvChildRequest(parent, "Child", 100, false, 6000, "Common.Approved");
        child.setId(2L);
        TvSeasonRequest season = new TvSeasonRequest(child, 7000, 1, false);
        season.setId(3L);
        TvEpisodeRequest episode = new TvEpisodeRequest(season, 8000, 1);
        episode.setId(4L);

        int h = episode.hashCode();
        h = assertChanged(h, episode, () -> episode.setOmbiEpisodeId(8001));
        h = assertChanged(h, episode, () -> episode.setOmbiEpisodeNumber(2));
        h = assertChanged(h, episode, () -> episode.setOmbiTitle("Pilot"));
        h = assertChanged(h, episode, () -> episode.setOmbiAvailable(true));
        h = assertChanged(h, episode, () -> episode.setOmbiApproved(true));
        h = assertChanged(h, episode, () -> episode.setOmbiRequested(true));
        h = assertChanged(h, episode, () -> episode.setOmbiRequestStatus("Common.Available"));
        h = assertChanged(h, episode, () -> episode.setSonarrPath("/tv/show/s01e01.mkv"));
        h = assertChanged(h, episode, () -> episode.setPlexPath("/plex/show/s01e01.mkv"));
        assertChanged(h, episode, () -> episode.setSonarrLastSearchTime(java.time.Instant.parse("2026-06-01T00:00:00Z")));
    }

    private static int assertChanged(int previousHash, Object entity, Runnable mutation) {
        mutation.run();
        int next = entity.hashCode();
        assertNotEquals(previousHash, next, "hashCode must change after mutating a refreshed field");
        return next;
    }
}
