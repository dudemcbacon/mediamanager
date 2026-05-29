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
    private Integer radarrHistoryCount;
    private Instant radarrLastSearched;

    @Column(columnDefinition = "TEXT")
    private String radarrPath;

    @Column(columnDefinition = "TEXT")
    private String radarrRootFolderPath;

    private String radarrOriginalLanguage;

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

    public Integer getRadarrHistoryCount() {
        return this.radarrHistoryCount;
    }

    public void setRadarrHistoryCount(Integer radarrHistoryCount) {
        this.radarrHistoryCount = radarrHistoryCount;
    }

    public Instant getRadarrLastSearched() {
        return this.radarrLastSearched;
    }

    public void setRadarrLastSearched(Instant radarrLastSearched) {
        this.radarrLastSearched = radarrLastSearched;
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

    public String getRadarrOriginalLanguage() {
        return this.radarrOriginalLanguage;
    }

    public void setRadarrOriginalLanguage(String radarrOriginalLanguage) {
        this.radarrOriginalLanguage = radarrOriginalLanguage;
    }

    @Override
    public boolean isAvailable() {
        return Boolean.TRUE.equals(this.radarrHasFile) && OMBI_AVAILABLE_STATUS.equals(getOmbiRequestStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getTitle(), getTmdbid());
    }

    @Override
    public String toString() {
        return String.format(
                "MovieRequest{id=%s, title=%s, tmdbid=%d, ombiAvailable=%b, ombiRequestId=%d, omviRequestStatus=%s}",
                getId(), getTitle(), getTmdbid(), getOmbiAvailable(), getOmbiRequestId(), getOmbiRequestStatus());
    }
}
