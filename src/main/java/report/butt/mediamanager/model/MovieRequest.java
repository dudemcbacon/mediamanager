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
@Table(name = "movie_request")
@DiscriminatorValue("MOVIE")
@OnDelete(action = OnDeleteAction.CASCADE)
public class MovieRequest extends Request {

    private Integer tmdbid;
    private Integer plexTmdbid;

    @Column(unique = true)
    private Integer radarrRequestId;

    private Boolean radarrHasFile;
    private Boolean radarrMonitored;
    private Boolean radarrIsAvailable;
    private Instant radarrLastSearchTime;

    @Column(columnDefinition = "TEXT")
    private String radarrPath;

    @Column(columnDefinition = "TEXT")
    private String radarrRootFolderPath;

    @Column(columnDefinition = "TEXT")
    private String radarrMovieFilePath;

    private String radarrOriginalLanguage;
    private String radarrQualityProfile;

    /** Whether {@link #radarrMovieFilePath} resolves to an existing file on the local filesystem (prefix applied). */
    private Boolean localFilePathAvailable;

    /** Size in bytes of the local file, when {@link #localFilePathAvailable} is true; otherwise null. */
    private Long localFileSize;

    MovieRequest() {}

    public MovieRequest(
            String title, Integer tmdbid, Boolean ombiAvailable, Integer ombiRequestId, String ombiRequestStatus) {
        setTitle(title);
        setTmdbid(tmdbid);
        setOmbiAvailable(ombiAvailable);
        setOmbiRequestId(ombiRequestId);
        setOmbiRequestStatus(ombiRequestStatus);
    }

    public Integer getTmdbid() {
        return this.tmdbid;
    }

    public void setTmdbid(Integer tmdbid) {
        this.tmdbid = tmdbid;
    }

    public Integer getPlexTmdbid() {
        return this.plexTmdbid;
    }

    public void setPlexTmdbid(Integer plexTmdbid) {
        this.plexTmdbid = plexTmdbid;
    }

    public Integer getRadarrRequestId() {
        return this.radarrRequestId;
    }

    public void setRadarrRequestId(Integer radarrRequestId) {
        this.radarrRequestId = radarrRequestId;
    }

    public Boolean getRadarrHasFile() {
        return this.radarrHasFile;
    }

    public void setRadarrHasFile(Boolean radarrHasFile) {
        this.radarrHasFile = radarrHasFile;
    }

    public Boolean getRadarrMonitored() {
        return this.radarrMonitored;
    }

    public void setRadarrMonitored(Boolean radarrMonitored) {
        this.radarrMonitored = radarrMonitored;
    }

    public Boolean getRadarrIsAvailable() {
        return this.radarrIsAvailable;
    }

    public void setRadarrIsAvailable(Boolean radarrIsAvailable) {
        this.radarrIsAvailable = radarrIsAvailable;
    }

    public Instant getRadarrLastSearchTime() {
        return this.radarrLastSearchTime;
    }

    public void setRadarrLastSearchTime(Instant radarrLastSearchTime) {
        this.radarrLastSearchTime = radarrLastSearchTime;
    }

    public String getRadarrPath() {
        return this.radarrPath;
    }

    public void setRadarrPath(String radarrPath) {
        this.radarrPath = radarrPath;
    }

    public String getRadarrRootFolderPath() {
        return this.radarrRootFolderPath;
    }

    public void setRadarrRootFolderPath(String radarrRootFolderPath) {
        this.radarrRootFolderPath = radarrRootFolderPath;
    }

    public String getRadarrMovieFilePath() {
        return this.radarrMovieFilePath;
    }

    public void setRadarrMovieFilePath(String radarrMovieFilePath) {
        this.radarrMovieFilePath = radarrMovieFilePath;
    }

    public String getRadarrOriginalLanguage() {
        return this.radarrOriginalLanguage;
    }

    public void setRadarrOriginalLanguage(String radarrOriginalLanguage) {
        this.radarrOriginalLanguage = radarrOriginalLanguage;
    }

    public String getRadarrQualityProfile() {
        return this.radarrQualityProfile;
    }

    public void setRadarrQualityProfile(String radarrQualityProfile) {
        this.radarrQualityProfile = radarrQualityProfile;
    }

    public Boolean getLocalFilePathAvailable() {
        return this.localFilePathAvailable;
    }

    public void setLocalFilePathAvailable(Boolean localFilePathAvailable) {
        this.localFilePathAvailable = localFilePathAvailable;
    }

    public Long getLocalFileSize() {
        return this.localFileSize;
    }

    public void setLocalFileSize(Long localFileSize) {
        this.localFileSize = localFileSize;
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
                getLocalFileSize());
    }

    @Override
    public String toString() {
        return String.format(
                "MovieRequest{id=%s, title=%s, tmdbid=%d, ombiAvailable=%b, ombiRequestId=%d, omviRequestStatus=%s}",
                getId(), getTitle(), getTmdbid(), getOmbiAvailable(), getOmbiRequestId(), getOmbiRequestStatus());
    }
}
