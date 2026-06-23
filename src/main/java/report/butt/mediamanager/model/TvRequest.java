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
@Table(name = "tv_request")
@DiscriminatorValue("TV")
@OnDelete(action = OnDeleteAction.CASCADE)
@NullMarked
public class TvRequest extends Request {

    private @Nullable Integer tvdbId;
    private @Nullable Integer plexTvdbId;

    // Not unique: a show can be requested in Ombi more than once (e.g. removed and re-requested,
    // yielding a new Ombi request id) while still mapping to the same Sonarr series. We persist one
    // row per Ombi request, so multiple rows may share a sonarrSeriesId.
    private @Nullable Integer sonarrSeriesId;

    private @Nullable String sonarrTitleSlug;

    private @Nullable Boolean sonarrMonitored;
    private @Nullable String sonarrMonitoredAll;

    @Column(columnDefinition = "TEXT")
    private @Nullable String sonarrPath;

    @Column(columnDefinition = "TEXT")
    private @Nullable String sonarrRootFolderPath;

    private @Nullable Integer sonarrEpisodeFileCount;
    private @Nullable Integer sonarrEpisodeCount;
    private @Nullable Integer sonarrTotalEpisodeCount;
    private @Nullable Instant sonarrLastSearched;
    private @Nullable String sonarrOriginalLanguage;
    private @Nullable String sonarrQualityProfile;

    private @Nullable Integer ombiTotalSeasons;
    private @Nullable Integer ombiExternalProviderId;

    TvRequest() {}

    public TvRequest(
            @Nullable String title,
            @Nullable Integer tvdbId,
            @Nullable Boolean ombiAvailable,
            @Nullable Integer ombiRequestId,
            @Nullable String ombiRequestStatus) {
        setTitle(title);
        setTvdbId(tvdbId);
        setOmbiAvailable(ombiAvailable);
        setOmbiRequestId(ombiRequestId);
        setOmbiRequestStatus(ombiRequestStatus);
    }

    public @Nullable Integer getTvdbId() {
        return this.tvdbId;
    }

    public void setTvdbId(@Nullable Integer tvdbId) {
        this.tvdbId = tvdbId;
    }

    public @Nullable Integer getPlexTvdbId() {
        return this.plexTvdbId;
    }

    public void setPlexTvdbId(@Nullable Integer plexTvdbId) {
        this.plexTvdbId = plexTvdbId;
    }

    public @Nullable Integer getSonarrSeriesId() {
        return this.sonarrSeriesId;
    }

    public void setSonarrSeriesId(@Nullable Integer sonarrSeriesId) {
        this.sonarrSeriesId = sonarrSeriesId;
    }

    public @Nullable String getSonarrTitleSlug() {
        return this.sonarrTitleSlug;
    }

    public void setSonarrTitleSlug(@Nullable String sonarrTitleSlug) {
        this.sonarrTitleSlug = sonarrTitleSlug;
    }

    public @Nullable Boolean getSonarrMonitored() {
        return this.sonarrMonitored;
    }

    public void setSonarrMonitored(@Nullable Boolean sonarrMonitored) {
        this.sonarrMonitored = sonarrMonitored;
    }

    public @Nullable String getSonarrMonitoredAll() {
        return this.sonarrMonitoredAll;
    }

    public void setSonarrMonitoredAll(@Nullable String sonarrMonitoredAll) {
        this.sonarrMonitoredAll = sonarrMonitoredAll;
    }

    public @Nullable String getSonarrPath() {
        return this.sonarrPath;
    }

    public void setSonarrPath(@Nullable String sonarrPath) {
        this.sonarrPath = sonarrPath;
    }

    public @Nullable String getSonarrRootFolderPath() {
        return this.sonarrRootFolderPath;
    }

    public void setSonarrRootFolderPath(@Nullable String sonarrRootFolderPath) {
        this.sonarrRootFolderPath = sonarrRootFolderPath;
    }

    public @Nullable Integer getSonarrEpisodeFileCount() {
        return this.sonarrEpisodeFileCount;
    }

    public void setSonarrEpisodeFileCount(@Nullable Integer sonarrEpisodeFileCount) {
        this.sonarrEpisodeFileCount = sonarrEpisodeFileCount;
    }

    public @Nullable Integer getSonarrEpisodeCount() {
        return this.sonarrEpisodeCount;
    }

    public void setSonarrEpisodeCount(@Nullable Integer sonarrEpisodeCount) {
        this.sonarrEpisodeCount = sonarrEpisodeCount;
    }

    public @Nullable Integer getSonarrTotalEpisodeCount() {
        return this.sonarrTotalEpisodeCount;
    }

    public void setSonarrTotalEpisodeCount(@Nullable Integer sonarrTotalEpisodeCount) {
        this.sonarrTotalEpisodeCount = sonarrTotalEpisodeCount;
    }

    public @Nullable Instant getSonarrLastSearched() {
        return this.sonarrLastSearched;
    }

    public void setSonarrLastSearched(@Nullable Instant sonarrLastSearched) {
        this.sonarrLastSearched = sonarrLastSearched;
    }

    public @Nullable String getSonarrOriginalLanguage() {
        return this.sonarrOriginalLanguage;
    }

    public void setSonarrOriginalLanguage(@Nullable String sonarrOriginalLanguage) {
        this.sonarrOriginalLanguage = sonarrOriginalLanguage;
    }

    public @Nullable String getSonarrQualityProfile() {
        return this.sonarrQualityProfile;
    }

    public void setSonarrQualityProfile(@Nullable String sonarrQualityProfile) {
        this.sonarrQualityProfile = sonarrQualityProfile;
    }

    public @Nullable Integer getOmbiTotalSeasons() {
        return this.ombiTotalSeasons;
    }

    public void setOmbiTotalSeasons(@Nullable Integer ombiTotalSeasons) {
        this.ombiTotalSeasons = ombiTotalSeasons;
    }

    public @Nullable Integer getOmbiExternalProviderId() {
        return this.ombiExternalProviderId;
    }

    public void setOmbiExternalProviderId(@Nullable Integer ombiExternalProviderId) {
        this.ombiExternalProviderId = ombiExternalProviderId;
    }

    @Override
    public boolean isAvailable() {
        return sonarrEpisodeFileCount != null
                && sonarrEpisodeCount != null
                && sonarrEpisodeCount > 0
                && sonarrEpisodeFileCount.intValue() >= sonarrEpisodeCount.intValue()
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
                getTvdbId(),
                getPlexTvdbId(),
                getSonarrSeriesId(),
                getSonarrTitleSlug(),
                getSonarrMonitored(),
                getSonarrMonitoredAll(),
                getSonarrPath(),
                getSonarrRootFolderPath(),
                getSonarrEpisodeFileCount(),
                getSonarrEpisodeCount(),
                getSonarrTotalEpisodeCount(),
                getSonarrLastSearched(),
                getSonarrOriginalLanguage(),
                getSonarrQualityProfile(),
                getOmbiTotalSeasons(),
                getOmbiExternalProviderId());
    }

    @Override
    public String toString() {
        return String.format(
                "TvRequest{id=%s, title=%s, tvdbId=%d, ombiAvailable=%b, ombiRequestId=%d, omviRequestStatus=%s}",
                getId(), getTitle(), getTvdbId(), getOmbiAvailable(), getOmbiRequestId(), getOmbiRequestStatus());
    }
}
