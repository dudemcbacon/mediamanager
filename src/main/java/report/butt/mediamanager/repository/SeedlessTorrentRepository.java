package report.butt.mediamanager.repository;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import report.butt.mediamanager.model.SeedlessTorrent;

@NullMarked
public interface SeedlessTorrentRepository extends JpaRepository<SeedlessTorrent, String> {}
