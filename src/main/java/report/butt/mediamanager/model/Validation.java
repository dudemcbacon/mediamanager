package report.butt.mediamanager.model;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
    name = "uk_validation_name_movie_request",
    columnNames = {"validation_name", "movie_request_id"}))
public class Validation {
  private @Id @GeneratedValue Long id;
  private String validationName;
  private Boolean result;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movie_request_id", nullable = false)
  private MovieRequest movieRequest;

  @CreationTimestamp
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  Validation() {
  }

  public Validation(String validationName, Boolean result, MovieRequest movieRequest) {
    this.validationName = validationName;
    this.result = result;
    this.movieRequest = movieRequest;
  }

  public Long getId() {
    return this.id;
  }

  public String getValidationName() {
    return this.validationName;
  }

  public Boolean getResult() {
    return this.result;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setValidationName(String validationName) {
    this.validationName = validationName;
  }

  public void setResult(Boolean result) {
    this.result = result;
  }

  public MovieRequest getMovieRequest() {
    return this.movieRequest;
  }

  public Instant getCreatedAt() {
    return this.createdAt;
  }

  public void setMovieRequest(MovieRequest movieRequest) {
    this.movieRequest = movieRequest;
  }

  public int hashCode() {
    return Objects.hash(this.id, this.validationName, this.result);
  }

  public String toString() {
    return String.format(
      "Validation{id=%d, validationName=%s, result=%b}",
    this.id, this.validationName, this.result);
  }
}
