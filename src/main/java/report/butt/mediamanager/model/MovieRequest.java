package report.butt.mediamanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "movie_request")
@DiscriminatorValue("MOVIE")
@OnDelete(action = OnDeleteAction.CASCADE)
@NullMarked
public class MovieRequest extends Request {

    private @Nullable Integer tmdbid;
    private @Nullable Integer plexTmdbid;

    @Column(unique = true)
    private @Nullable Integer radarrRequestId;

    private @Nullable Boolean radarrHasFile;
    private @Nullable Boolean radarrMonitored;
    private @Nullable Boolean radarrIsAvailable;
    private @Nullable Instant radarrLastSearchTime;

    @Column(columnDefinition = "TEXT")
    private @Nullable String radarrPath;

    @Column(columnDefinition = "TEXT")
    private @Nullable String radarrRootFolderPath;

    @Column(columnDefinition = "TEXT")
    private @Nullable String radarrMovieFilePath;

    private @Nullable String radarrOriginalLanguage;
    private @Nullable String radarrQualityProfile;

    /** Whether {@link #radarrMovieFilePath} resolves to an existing file on the local filesystem (prefix applied). */
    private @Nullable Boolean localFilePathAvailable;

    /** Size in bytes of the local file, when {@link #localFilePathAvailable} is true; otherwise null. */
    private @Nullable Long localFileSize;

    /** Tdarr's health-check verdict for the media file, e.g. {@code Success} or {@code Queued}. */
    private @Nullable String tdarrHealthCheck;

    /** Tdarr's transcode verdict for the media file, e.g. {@code Transcode success} or {@code Not required}. */
    private @Nullable String tdarrTranscodeDecisionMaker;

    /** Pre-transcode size in GiB as reported by Tdarr (its own unit — not bytes, unlike {@link #localFileSize}). */
    private @Nullable Double tdarrOldSizeGb;

    /** Post-transcode size in GiB as reported by Tdarr; same unit as {@link #tdarrOldSizeGb}. */
    private @Nullable Double tdarrNewSizeGb;

    /**
     * When Tdarr data was last successfully applied to this movie — either a refresh reading it from Tdarr or the
     * transcode-complete webhook pushing it in. A freshness stamp, so it moves even when the four values are unchanged;
     * unlike {@code updatedAt} it tracks only Tdarr, and unlike the values themselves it stays null until Tdarr has
     * actually reported on the file.
     */
    private @Nullable Instant tdarrLastUpdated;

    /**
     * How many searches this app has requested for the movie. Null means we have never searched for it, as distinct
     * from a zero count. Counts only our own requests, unlike {@link #radarrLastSearchTime}, which Radarr overwrites
     * on every refresh with whenever it last searched by any trigger.
     */
    private @Nullable Integer searchCount;

    /** When this app first requested a search for the movie; set once and never moved. */
    private @Nullable Instant searchFirstAt;

    /**
     * When this app last requested a search for the movie. The span from {@link #searchFirstAt} to here is how long we
     * have been searching fruitlessly; once it exceeds {@code search.stale-after-days} the request is marked stale.
     */
    private @Nullable Instant searchLastAt;

    MovieRequest() {}

    public MovieRequest(
            @Nullable String title,
            @Nullable Integer tmdbid,
            @Nullable Boolean ombiAvailable,
            @Nullable Integer ombiRequestId,
            @Nullable String ombiRequestStatus) {
        setTitle(title);
        setTmdbid(tmdbid);
        setOmbiAvailable(ombiAvailable);
        setOmbiRequestId(ombiRequestId);
        setOmbiRequestStatus(ombiRequestStatus);
    }

    public @Nullable Integer getTmdbid() {
        return this.tmdbid;
    }

    public void setTmdbid(@Nullable Integer tmdbid) {
        this.tmdbid = tmdbid;
    }

    public @Nullable Integer getPlexTmdbid() {
        return this.plexTmdbid;
    }

    public void setPlexTmdbid(@Nullable Integer plexTmdbid) {
        this.plexTmdbid = plexTmdbid;
    }

    public @Nullable Integer getRadarrRequestId() {
        return this.radarrRequestId;
    }

    public void setRadarrRequestId(@Nullable Integer radarrRequestId) {
        this.radarrRequestId = radarrRequestId;
    }

    public @Nullable Boolean getRadarrHasFile() {
        return this.radarrHasFile;
    }

    public void setRadarrHasFile(@Nullable Boolean radarrHasFile) {
        this.radarrHasFile = radarrHasFile;
    }

    public @Nullable Boolean getRadarrMonitored() {
        return this.radarrMonitored;
    }

    public void setRadarrMonitored(@Nullable Boolean radarrMonitored) {
        this.radarrMonitored = radarrMonitored;
    }

    public @Nullable Boolean getRadarrIsAvailable() {
        return this.radarrIsAvailable;
    }

    public void setRadarrIsAvailable(@Nullable Boolean radarrIsAvailable) {
        this.radarrIsAvailable = radarrIsAvailable;
    }

    public @Nullable Instant getRadarrLastSearchTime() {
        return this.radarrLastSearchTime;
    }

    public void setRadarrLastSearchTime(@Nullable Instant radarrLastSearchTime) {
        this.radarrLastSearchTime = radarrLastSearchTime;
    }

    public @Nullable String getRadarrPath() {
        return this.radarrPath;
    }

    public void setRadarrPath(@Nullable String radarrPath) {
        this.radarrPath = radarrPath;
    }

    public @Nullable String getRadarrRootFolderPath() {
        return this.radarrRootFolderPath;
    }

    public void setRadarrRootFolderPath(@Nullable String radarrRootFolderPath) {
        this.radarrRootFolderPath = radarrRootFolderPath;
    }

    public @Nullable String getRadarrMovieFilePath() {
        return this.radarrMovieFilePath;
    }

    public void setRadarrMovieFilePath(@Nullable String radarrMovieFilePath) {
        this.radarrMovieFilePath = radarrMovieFilePath;
    }

    public @Nullable String getRadarrOriginalLanguage() {
        return this.radarrOriginalLanguage;
    }

    public void setRadarrOriginalLanguage(@Nullable String radarrOriginalLanguage) {
        this.radarrOriginalLanguage = radarrOriginalLanguage;
    }

    public @Nullable String getRadarrQualityProfile() {
        return this.radarrQualityProfile;
    }

    public void setRadarrQualityProfile(@Nullable String radarrQualityProfile) {
        this.radarrQualityProfile = radarrQualityProfile;
    }

    public @Nullable Boolean getLocalFilePathAvailable() {
        return this.localFilePathAvailable;
    }

    public void setLocalFilePathAvailable(@Nullable Boolean localFilePathAvailable) {
        this.localFilePathAvailable = localFilePathAvailable;
    }

    public @Nullable Long getLocalFileSize() {
        return this.localFileSize;
    }

    public void setLocalFileSize(@Nullable Long localFileSize) {
        this.localFileSize = localFileSize;
    }

    public @Nullable String getTdarrHealthCheck() {
        return this.tdarrHealthCheck;
    }

    public void setTdarrHealthCheck(@Nullable String tdarrHealthCheck) {
        this.tdarrHealthCheck = tdarrHealthCheck;
    }

    public @Nullable String getTdarrTranscodeDecisionMaker() {
        return this.tdarrTranscodeDecisionMaker;
    }

    public void setTdarrTranscodeDecisionMaker(@Nullable String tdarrTranscodeDecisionMaker) {
        this.tdarrTranscodeDecisionMaker = tdarrTranscodeDecisionMaker;
    }

    public @Nullable Double getTdarrOldSizeGb() {
        return this.tdarrOldSizeGb;
    }

    public void setTdarrOldSizeGb(@Nullable Double tdarrOldSizeGb) {
        this.tdarrOldSizeGb = tdarrOldSizeGb;
    }

    public @Nullable Double getTdarrNewSizeGb() {
        return this.tdarrNewSizeGb;
    }

    public void setTdarrNewSizeGb(@Nullable Double tdarrNewSizeGb) {
        this.tdarrNewSizeGb = tdarrNewSizeGb;
    }

    public @Nullable Instant getTdarrLastUpdated() {
        return this.tdarrLastUpdated;
    }

    public void setTdarrLastUpdated(@Nullable Instant tdarrLastUpdated) {
        this.tdarrLastUpdated = tdarrLastUpdated;
    }

    public @Nullable Integer getSearchCount() {
        return this.searchCount;
    }

    public void setSearchCount(@Nullable Integer searchCount) {
        this.searchCount = searchCount;
    }

    public @Nullable Instant getSearchFirstAt() {
        return this.searchFirstAt;
    }

    public void setSearchFirstAt(@Nullable Instant searchFirstAt) {
        this.searchFirstAt = searchFirstAt;
    }

    public @Nullable Instant getSearchLastAt() {
        return this.searchLastAt;
    }

    public void setSearchLastAt(@Nullable Instant searchLastAt) {
        this.searchLastAt = searchLastAt;
    }

    @Override
    public boolean isAvailable() {
        return Objects.equals(this.radarrHasFile, true)
                && Objects.equals(getOmbiRequestStatus(), OMBI_AVAILABLE_STATUS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getId(),
                getTitle(),
                getOmbiAvailable(),
                getOmbiRequestId(),
                getOmbiRequestStatus(),
                getOmbiUserName(),
                getOmbiRequestedDate(),
                getStale(),
                getStaleReason(),
                getMarkedStaleAt(),
                getPlexMetadataUrl(),
                getPlexMetadataId(),
                getPlexAddedAt(),
                getPlexUpdatedAt(),
                getPlexMediaId(),
                getPlexMediaFilename(),
                getPlexMediaSize(),
                getPlexMediaDuration(),
                getCreatedAt(),
                getUpdatedAt(),
                getTmdbid(),
                getPlexTmdbid(),
                getRadarrRequestId(),
                getRadarrHasFile(),
                getRadarrMonitored(),
                getRadarrIsAvailable(),
                getRadarrLastSearchTime(),
                getRadarrPath(),
                getRadarrRootFolderPath(),
                getRadarrMovieFilePath(),
                getRadarrOriginalLanguage(),
                getRadarrQualityProfile(),
                getLocalFilePathAvailable(),
                getLocalFileSize(),
                getTdarrHealthCheck(),
                getTdarrTranscodeDecisionMaker(),
                getTdarrOldSizeGb(),
                getTdarrNewSizeGb(),
                getTdarrLastUpdated(),
                getSearchCount(),
                getSearchFirstAt(),
                getSearchLastAt());
    }

    @Override
    public String toString() {
        return String.format(
                "MovieRequest{id=%s, title=%s, tmdbid=%d, ombiAvailable=%b, ombiRequestId=%d, omviRequestStatus=%s}",
                getId(), getTitle(), getTmdbid(), getOmbiAvailable(), getOmbiRequestId(), getOmbiRequestStatus());
    }
}
