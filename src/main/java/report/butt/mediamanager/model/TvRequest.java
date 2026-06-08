package report.butt.mediamanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "tv_request")
@DiscriminatorValue("TV")
@OnDelete(action = OnDeleteAction.CASCADE)
public class TvRequest extends Request {

    private Integer tvdbId;
    private Integer plexTvdbId;

    // Not unique: a show can be requested in Ombi more than once (e.g. removed and re-requested,
    // yielding a new Ombi request id) while still mapping to the same Sonarr series. We persist one
    // row per Ombi request, so multiple rows may share a sonarrSeriesId.
    private Integer sonarrSeriesId;

    private String sonarrTitleSlug;

    private Boolean sonarrMonitored;
    private String sonarrMonitoredAll;

    @Column(columnDefinition = "TEXT")
    private String sonarrPath;

    @Column(columnDefinition = "TEXT")
    private String sonarrRootFolderPath;

    private Integer sonarrEpisodeFileCount;
    private Integer sonarrEpisodeCount;
    private Integer sonarrTotalEpisodeCount;
    private Instant sonarrLastSearched;
    private String sonarrOriginalLanguage;
    private String sonarrQualityProfile;

    private Integer ombiTotalSeasons;
    private Integer ombiExternalProviderId;

    TvRequest() {}

    public TvRequest(
            String title, Integer tvdbId, Boolean ombiAvailable, Integer ombiRequestId, String ombiRequestStatus) {
        setTitle(title);
        setTvdbId(tvdbId);
        setOmbiAvailable(ombiAvailable);
        setOmbiRequestId(ombiRequestId);
        setOmbiRequestStatus(ombiRequestStatus);
    }

    public Integer getTvdbId() {
        return this.tvdbId;
    }

    public void setTvdbId(Integer tvdbId) {
        this.tvdbId = tvdbId;
    }

    public Integer getPlexTvdbId() {
        return this.plexTvdbId;
    }

    public void setPlexTvdbId(Integer plexTvdbId) {
        this.plexTvdbId = plexTvdbId;
    }

    public Integer getSonarrSeriesId() {
        return this.sonarrSeriesId;
    }

    public void setSonarrSeriesId(Integer sonarrSeriesId) {
        this.sonarrSeriesId = sonarrSeriesId;
    }

    public String getSonarrTitleSlug() {
        return this.sonarrTitleSlug;
    }

    public void setSonarrTitleSlug(String sonarrTitleSlug) {
        this.sonarrTitleSlug = sonarrTitleSlug;
    }

    public Boolean getSonarrMonitored() {
        return this.sonarrMonitored;
    }

    public void setSonarrMonitored(Boolean sonarrMonitored) {
        this.sonarrMonitored = sonarrMonitored;
    }

    public String getSonarrMonitoredAll() {
        return this.sonarrMonitoredAll;
    }

    public void setSonarrMonitoredAll(String sonarrMonitoredAll) {
        this.sonarrMonitoredAll = sonarrMonitoredAll;
    }

    public String getSonarrPath() {
        return this.sonarrPath;
    }

    public void setSonarrPath(String sonarrPath) {
        this.sonarrPath = sonarrPath;
    }

    public String getSonarrRootFolderPath() {
        return this.sonarrRootFolderPath;
    }

    public void setSonarrRootFolderPath(String sonarrRootFolderPath) {
        this.sonarrRootFolderPath = sonarrRootFolderPath;
    }

    public Integer getSonarrEpisodeFileCount() {
        return this.sonarrEpisodeFileCount;
    }

    public void setSonarrEpisodeFileCount(Integer sonarrEpisodeFileCount) {
        this.sonarrEpisodeFileCount = sonarrEpisodeFileCount;
    }

    public Integer getSonarrEpisodeCount() {
        return this.sonarrEpisodeCount;
    }

    public void setSonarrEpisodeCount(Integer sonarrEpisodeCount) {
        this.sonarrEpisodeCount = sonarrEpisodeCount;
    }

    public Integer getSonarrTotalEpisodeCount() {
        return this.sonarrTotalEpisodeCount;
    }

    public void setSonarrTotalEpisodeCount(Integer sonarrTotalEpisodeCount) {
        this.sonarrTotalEpisodeCount = sonarrTotalEpisodeCount;
    }

    public Instant getSonarrLastSearched() {
        return this.sonarrLastSearched;
    }

    public void setSonarrLastSearched(Instant sonarrLastSearched) {
        this.sonarrLastSearched = sonarrLastSearched;
    }

    public String getSonarrOriginalLanguage() {
        return this.sonarrOriginalLanguage;
    }

    public void setSonarrOriginalLanguage(String sonarrOriginalLanguage) {
        this.sonarrOriginalLanguage = sonarrOriginalLanguage;
    }

    public String getSonarrQualityProfile() {
        return this.sonarrQualityProfile;
    }

    public void setSonarrQualityProfile(String sonarrQualityProfile) {
        this.sonarrQualityProfile = sonarrQualityProfile;
    }

    public Integer getOmbiTotalSeasons() {
        return this.ombiTotalSeasons;
    }

    public void setOmbiTotalSeasons(Integer ombiTotalSeasons) {
        this.ombiTotalSeasons = ombiTotalSeasons;
    }

    public Integer getOmbiExternalProviderId() {
        return this.ombiExternalProviderId;
    }

    public void setOmbiExternalProviderId(Integer ombiExternalProviderId) {
        this.ombiExternalProviderId = ombiExternalProviderId;
    }

    @Override
    public boolean isAvailable() {
        return sonarrEpisodeFileCount != null
                && sonarrEpisodeCount != null
                && sonarrEpisodeCount > 0
                && sonarrEpisodeFileCount.intValue() >= sonarrEpisodeCount.intValue()
                && OMBI_AVAILABLE_STATUS.equals(getOmbiRequestStatus());
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
