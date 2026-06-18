package report.butt.mediamanager.validation;

import java.util.Objects;
import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
public class SonarrMonitored implements Validator<TvRequest> {

    private static final String MONITOR_ALL = "all";

    @Override
    public Boolean validate(TvRequest request) {
        return Objects.equals(request.getSonarrMonitored(), true)
                && Objects.equals(request.getSonarrMonitoredAll(), MONITOR_ALL);
    }

    @Override
    public RequestType supportedType() {
        return RequestType.TV;
    }

    @Override
    public int sortOrder() {
        return 800;
    }

    @Override
    public String shortName() {
        return "Monit";
    }

    @Override
    public String title() {
        return "Monitored?";
    }

    @Override
    public String description() {
        return "Sonarr is monitoring the series and set to monitor all new items.";
    }
}
