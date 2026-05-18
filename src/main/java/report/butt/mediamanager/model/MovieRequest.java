package report.butt.mediamanager.model;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class MovieRequest {

  private @Id @GeneratedValue Long id;
  private String title;
  private Integer tmdbid;
  private Boolean ombiAvailable;
  private Integer ombiRequestId;
  private String ombiRequestStatus;

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

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.title, this.tmdbid);
  }

  @Override
  public String toString() {
    return "MovieRequest{id=" + this.id + ", title='" + this.title + "', role='" + this.tmdbid + "'}";
  }
}
