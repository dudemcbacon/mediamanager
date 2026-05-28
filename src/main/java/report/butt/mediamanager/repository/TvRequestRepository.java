package report.butt.mediamanager.repository;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import report.butt.mediamanager.model.TvRequest;

@NullMarked
public interface TvRequestRepository extends JpaRepository<TvRequest, Long> {

  @Query("SELECT t FROM TvRequest t WHERE TYPE(t) = TvRequest AND t.ombiRequestId = :ombiRequestId")
  Optional<TvRequest> findByOmbiRequestId(@Param("ombiRequestId") Integer ombiRequestId);

  @Override
  @Query("SELECT t FROM TvRequest t WHERE TYPE(t) = TvRequest")
  List<TvRequest> findAll();

}
