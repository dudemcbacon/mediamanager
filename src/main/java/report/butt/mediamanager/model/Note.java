package report.butt.mediamanager.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Note {

  private @Id @GeneratedValue Long id;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movie_request_id", nullable = false)
  private MovieRequest movieRequest;

  @CreationTimestamp
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  Note() {
  }

  public Note(String notes, MovieRequest movieRequest) {
    this.notes = notes;
    this.movieRequest = movieRequest;
  }

  public Long getId() {
    return id;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public MovieRequest getMovieRequest() {
    return movieRequest;
  }

  public void setMovieRequest(MovieRequest movieRequest) {
    this.movieRequest = movieRequest;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
