package report.butt.mediamanager.repository;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import report.butt.mediamanager.model.RemovedDownload;

@NullMarked
public interface RemovedDownloadRepository extends JpaRepository<RemovedDownload, String> {}
