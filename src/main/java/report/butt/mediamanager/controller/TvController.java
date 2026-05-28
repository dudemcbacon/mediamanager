package report.butt.mediamanager.controller;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.transaction.annotation.Transactional;

import report.butt.mediamanager.client.OmbiClient;
import report.butt.mediamanager.client.SonarrClient;
import report.butt.mediamanager.exceptions.RequestNotFoundException;
import report.butt.mediamanager.model.TvRequest;
import report.butt.mediamanager.model.ombi.OmbiReprocessResponse;
import report.butt.mediamanager.model.sonarr.SonarrCommand;
import report.butt.mediamanager.model.Note;
import report.butt.mediamanager.repository.TvRequestRepository;
import report.butt.mediamanager.repository.NoteRepository;
import report.butt.mediamanager.repository.ValidationRepository;
import report.butt.mediamanager.service.ValidatorService;
import report.butt.mediamanager.service.TvRefreshService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Controller
public class TvController {

  private static final Logger log = LoggerFactory.getLogger(TvController.class);

  private final TvRequestRepository tvRequestRepository;
  private final ValidationRepository validationRepository;
  private final NoteRepository noteRepository;
  private final OmbiClient ombiClient;
  private final SonarrClient sonarrClient;
  private final ObjectMapper objectMapper;
  private final TvRefreshService tvRefreshService;
  private final ValidatorService validatorService;

  @Autowired
  public TvController(TvRequestRepository tvRequestRepository,
      ValidationRepository validationRepository, NoteRepository noteRepository,
      OmbiClient ombiClient, SonarrClient sonarrClient, ObjectMapper objectMapper,
      TvRefreshService tvRefreshService, ValidatorService validatorService) {
    this.tvRequestRepository = tvRequestRepository;
    this.validationRepository = validationRepository;
    this.noteRepository = noteRepository;
    this.ombiClient = ombiClient;
    this.sonarrClient = sonarrClient;
    this.objectMapper = objectMapper;
    this.tvRefreshService = tvRefreshService;
    this.validatorService = validatorService;
  }

  @PostMapping("/tv/refresh-all")
  public String refreshAll() {
    log.info("Refresh-all request");
    tvRefreshService.refreshAll();
    return "redirect:/tv";
  }

  @PostMapping("/tv/search-missing")
  public String searchMissing() {
    List<TvRequest> tvRequests = tvRequestRepository.findAll().stream()
        .filter(tvRequest -> "Common.ProcessingRequest".equals(tvRequest.getOmbiRequestStatus())
            && tvRequest.getSonarrEpisodeFileCount() != null
            && tvRequest.getSonarrTotalEpisodeCount() != null
            && tvRequest.getSonarrTotalEpisodeCount() > 0
            && tvRequest.getSonarrEpisodeFileCount() < tvRequest.getSonarrTotalEpisodeCount()
            && tvRequest.getSonarrSeriesId() != null)
        .toList();

    List<Integer> seriesIds = tvRequests.stream().map(TvRequest::getSonarrSeriesId).toList();

    log.info("Triggering Sonarr SeriesSearch for {} series: {}", seriesIds.size(), seriesIds);
    if (!seriesIds.isEmpty()) {
      SonarrCommand command = sonarrClient.searchSeries(seriesIds);
      log.info("Sonarr command {} ({}) status={} result={}", command.getId(), command.getCommandName(),
          command.getStatus(), command.getResult());

      Instant now = Instant.now();
      tvRequests.forEach(tvRequest -> tvRequest.setSonarrLastSearched(now));
      tvRequestRepository.saveAll(tvRequests);
    }

    return "redirect:/tv";
  }

  @GetMapping("/tv")
  public String tv(Model model) {
    List<TvRequest> tvRequests = tvRequestRepository.findAll().stream()
        .sorted(Comparator.comparing(TvRequest::getSonarrSeriesId,
            Comparator.nullsFirst(Comparator.naturalOrder())))
        .toList();

    model.addAttribute("tvRequests", tvRequests);
    return "tv";
  }

  @PostMapping("/tv/{seriesId}/search")
  public String search(@PathVariable Integer seriesId) {
    log.info("Triggering Sonarr SeriesSearch for series {}", seriesId);
    SonarrCommand command = sonarrClient.searchSeries(List.of(seriesId));
    log.info("Sonarr command {} ({}) status={} result={}", command.getId(), command.getCommandName(),
        command.getStatus(), command.getResult());

    Instant now = Instant.now();
    tvRequestRepository.findAll().stream()
        .filter(tvRequest -> seriesId.equals(tvRequest.getSonarrSeriesId()))
        .forEach(tvRequest -> {
          tvRequest.setSonarrLastSearched(now);
          tvRequestRepository.save(tvRequest);
        });

    return "redirect:/tv";
  }

  @PostMapping("/tv/{id}/refresh")
  public String refresh(@PathVariable Long id) {
    log.info("Refresh request for tv request {}", id);
    tvRefreshService.refreshOne(id);
    return "redirect:/tv";
  }

  @PostMapping("/tv/{id}/validate")
  public String validate(@PathVariable Long id) {
    log.info("Validate request for tv request {}", id);
    TvRequest tvRequest = tvRequestRepository.findById(id)
        .orElseThrow(() -> new RequestNotFoundException(id));
    validatorService.validate(tvRequest);
    return "redirect:/tv";
  }

  @PostMapping("/tv/validate-all")
  public String validateAll() {
    log.info("Validate-all request");
    tvRequestRepository.findAll().forEach(validatorService::validate);
    return "redirect:/tv";
  }

  @PostMapping("/tv/{id}/search-one")
  public String searchOne(@PathVariable Long id) {
    log.info("Search request for tv request {}", id);
    TvRequest tvRequest = tvRequestRepository.findById(id)
        .orElseThrow(() -> new RequestNotFoundException(id));

    Integer sonarrSeriesId = tvRequest.getSonarrSeriesId();
    if (sonarrSeriesId == null) {
      log.warn("TvRequest {} ({}) has no sonarrSeriesId; skipping search", id, tvRequest.getTitle());
      return "redirect:/tv";
    }

    SonarrCommand command = sonarrClient.searchSeries(List.of(sonarrSeriesId));
    log.info("Sonarr command {} ({}) status={} result={}", command.getId(), command.getCommandName(),
        command.getStatus(), command.getResult());

    tvRequest.setSonarrLastSearched(Instant.now());
    tvRequestRepository.save(tvRequest);
    return "redirect:/tv";
  }

  @PostMapping("/tv/search-all")
  public String searchAll() {
    log.info("Search-all request");
    List<TvRequest> tvRequests = tvRequestRepository.findAll().stream()
        .filter(tr -> tr.getSonarrSeriesId() != null)
        .toList();

    if (tvRequests.isEmpty()) {
      log.info("No tv requests with a sonarrSeriesId; nothing to search");
      return "redirect:/tv";
    }

    List<Integer> seriesIds = tvRequests.stream().map(TvRequest::getSonarrSeriesId).toList();
    log.info("Triggering Sonarr SeriesSearch for {} series: {}", seriesIds.size(), seriesIds);
    SonarrCommand command = sonarrClient.searchSeries(seriesIds);
    log.info("Sonarr command {} ({}) status={} result={}", command.getId(), command.getCommandName(),
        command.getStatus(), command.getResult());

    Instant now = Instant.now();
    tvRequests.forEach(tr -> tr.setSonarrLastSearched(now));
    tvRequestRepository.saveAll(tvRequests);
    return "redirect:/tv";
  }

  @PostMapping("/tv/{id}/mark-available")
  public String markAvailable(@PathVariable Long id) {
    log.info("Mark available request for tv request {}", id);
    TvRequest tvRequest = tvRequestRepository.findById(id)
        .orElseThrow(() -> new RequestNotFoundException(id));

    log.info("Found TvRequest for {}", tvRequest.getTitle());

    OmbiReprocessResponse response = ombiClient.markTvAvailable(tvRequest.getOmbiRequestId());
    try {
      log.info("Ombi mark-available response for tv request {} ({}): {}", id, tvRequest.getTitle(),
          objectMapper.writeValueAsString(response));
    } catch (JacksonException e) {
      log.warn("Failed to serialize Ombi mark-available response for tv request {} ({})", id, tvRequest.getTitle(), e);
    }

    return "redirect:/tv";
  }

  @PostMapping("/tv/{id}/delete")
  @Transactional
  public String delete(@PathVariable Long id) {
    log.info("Delete request for tv request {}", id);
    TvRequest tvRequest = tvRequestRepository.findById(id)
        .orElseThrow(() -> new RequestNotFoundException(id));
    validationRepository.deleteByRequest(tvRequest);
    noteRepository.deleteByRequest(tvRequest);
    tvRequestRepository.delete(tvRequest);
    return "redirect:/tv";
  }

  @PostMapping("/tv/{id}/notes")
  public Note addNote(@PathVariable Long id, @RequestParam("notes") String notes) {
    log.info("Add note request for tv request {}", id);
    TvRequest tvRequest = tvRequestRepository.findById(id)
        .orElseThrow(() -> new RequestNotFoundException(id));
    return noteRepository.save(new Note(notes, tvRequest));
  }

  @PostMapping("/tv/{id}/mark-stale")
  public String markStale(@PathVariable Long id, @RequestParam("reason") String reason) {
    log.info("Mark stale request for tv request {} with reason: {}", id, reason);
    TvRequest tvRequest = tvRequestRepository.findById(id)
        .orElseThrow(() -> new RequestNotFoundException(id));

    tvRequest.setStale(true);
    tvRequest.setStaleReason(reason);
    tvRequest.setMarkedStaleAt(Instant.now());
    tvRequestRepository.save(tvRequest);

    return "redirect:/tv";
  }
}
