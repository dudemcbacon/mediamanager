package report.butt.mediamanager.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "request") // Update this line
public class MovieRequest {

  private static final String OMBI_AVAILABLE_STATUS = "Common.Available";

  private @Id @GeneratedValue Long id;
  private String title;
  private Integer tmdbid;
  private Boolean ombiAvailable;

  @Column(unique = true)
  private Integer ombiRequestId;
  private String ombiRequestStatus;
  private String ombiUserName;

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

  private Boolean stale;

  @Column(columnDefinition = "TEXT")
  private String staleReason;

  private Instant markedStaleAt;

  @Column(columnDefinition = "TEXT")
  private String plexMetadataUrl;

  private String plexMetadataId;
  private Integer plexTmdbid;
  private Long plexAddedAt;
  private Long plexUpdatedAt;
  private Integer plexMediaId;

  @Column(columnDefinition = "TEXT")
  private String plexMediaFilename;

  private Long plexMediaSize;
  private Long plexMediaDuration;

  @CreationTimestamp
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  MovieRequest() {
  }

  public MovieRequest(String title, Integer tmdbid, Boolean ombiAvailable, Integer ombiRequestId,
      String ombiRequestStatus) {
    this.title = title;
    this.tmdbid = tmdbid;
    this.ombiAvailable = ombiAvailable;
    this.ombiRequestId = ombiRequestId;
    this.ombiRequestStatus = ombiRequestStatus;
  }

  public Long getId() {
    return this.id;
  }

  public String getTitle() {
    return this.title;
  }

  public Integer getTmdbid() {
    return this.tmdbid;
  }

  public Boolean getOmbiAvailable() {
    return this.ombiAvailable;
  }

  public Integer getOmbiRequestId() {
    return this.ombiRequestId;
  }

  public String getOmbiRequestStatus() {
    return this.ombiRequestStatus;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setTmdbid(Integer tmdbid) {
    this.tmdbid = tmdbid;
  }

  public void setOmbiAvailable(Boolean ombiAvailable) {
    this.ombiAvailable = ombiAvailable;
  }

  public void setOmbiRequestId(Integer ombiRequestId) {
    this.ombiRequestId = ombiRequestId;
  }

  public void setOmbiRequestStatus(String ombiRequestStatus) {
    this.ombiRequestStatus = ombiRequestStatus;
  }

  public String getOmbiUserName() {
    return this.ombiUserName;
  }

  public void setOmbiUserName(String ombiUserName) {
    this.ombiUserName = ombiUserName;
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

  public Boolean getStale() {
    return this.stale;
  }

  public void setStale(Boolean stale) {
    this.stale = stale;
  }

  public String getStaleReason() {
    return this.staleReason;
  }

  public void setStaleReason(String staleReason) {
    this.staleReason = staleReason;
  }

  public Instant getMarkedStaleAt() {
    return this.markedStaleAt;
  }

  public void setMarkedStaleAt(Instant markedStaleAt) {
    this.markedStaleAt = markedStaleAt;
  }

  public String getPlexMetadataUrl() {
    return this.plexMetadataUrl;
  }

  public void setPlexMetadataUrl(String plexMetadataUrl) {
    this.plexMetadataUrl = plexMetadataUrl;
  }

  public String getPlexMetadataId() {
    return this.plexMetadataId;
  }

  public void setPlexMetadataId(String plexMetadataId) {
    this.plexMetadataId = plexMetadataId;
  }

  public Integer getPlexTmdbid() {
    return this.plexTmdbid;
  }

  public void setPlexTmdbid(Integer plexTmdbid) {
    this.plexTmdbid = plexTmdbid;
  }

  public Long getPlexAddedAt() {
    return this.plexAddedAt;
  }

  public void setPlexAddedAt(Long plexAddedAt) {
    this.plexAddedAt = plexAddedAt;
  }

  public Long getPlexUpdatedAt() {
    return this.plexUpdatedAt;
  }

  public void setPlexUpdatedAt(Long plexUpdatedAt) {
    this.plexUpdatedAt = plexUpdatedAt;
  }

  public Integer getPlexMediaId() {
    return this.plexMediaId;
  }

  public void setPlexMediaId(Integer plexMediaId) {
    this.plexMediaId = plexMediaId;
  }

  public String getPlexMediaFilename() {
    return this.plexMediaFilename;
  }

  public void setPlexMediaFilename(String plexMediaFilename) {
    this.plexMediaFilename = plexMediaFilename;
  }

  public Long getPlexMediaSize() {
    return this.plexMediaSize;
  }

  public void setPlexMediaSize(Long plexMediaSize) {
    this.plexMediaSize = plexMediaSize;
  }

  public Long getPlexMediaDuration() {
    return this.plexMediaDuration;
  }

  public void setPlexMediaDuration(Long plexMediaDuration) {
    this.plexMediaDuration = plexMediaDuration;
  }

  public Instant getCreatedAt() {
    return this.createdAt;
  }

  public Instant getUpdatedAt() {
    return this.updatedAt;
  }

  public boolean isAvailable() {
    return Boolean.TRUE.equals(this.radarrHasFile)
        && OMBI_AVAILABLE_STATUS.equals(this.ombiRequestStatus);
  }

  public boolean isValid(Collection<String> validatorNames, Map<String, Validation> latestByName) {
    return validatorNames.stream().allMatch(name -> {
      Validation v = latestByName.get(name);
      return v != null && Boolean.TRUE.equals(v.getResult());
    });
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.title, this.tmdbid);
  }

  @Override
  public String toString() {
    return String.format(
        "MovieRequest{id=%s, title=%s, tmdbid=%d, ombiAvailable=%b, ombiRequestId=%d, omviRequestStatus=%s}", this.id,
        this.title, this.tmdbid, this.ombiAvailable, this.ombiRequestId, this.ombiRequestStatus);
  }
}
