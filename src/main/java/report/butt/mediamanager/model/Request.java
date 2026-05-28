package report.butt.mediamanager.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "request", uniqueConstraints = @UniqueConstraint(
    name = "uk_request_ombi_id_type",
    columnNames = { "ombi_request_id", "request_type" }))
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "request_type")
public abstract class Request {

  protected static final String OMBI_AVAILABLE_STATUS = "Common.Available";

  private @Id @GeneratedValue Long id;
  private String title;
  private Boolean ombiAvailable;

  private Integer ombiRequestId;
  private String ombiRequestStatus;
  private String ombiUserName;

  private Boolean stale;

  @Column(columnDefinition = "TEXT")
  private String staleReason;

  private Instant markedStaleAt;

  @Column(columnDefinition = "TEXT")
  private String plexMetadataUrl;

  private String plexMetadataId;
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

  public abstract boolean isAvailable();

  public Long getId() {
    return this.id;
  }

  public String getTitle() {
    return this.title;
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

  public boolean isValid(Collection<String> validatorNames, Map<String, Validation> latestByName) {
    return validatorNames.stream().allMatch(name -> {
      Validation v = latestByName.get(name);
      return v != null && Boolean.TRUE.equals(v.getResult());
    });
  }
}
