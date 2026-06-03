package report.butt.mediamanager.validation;

import org.springframework.stereotype.Component;
import report.butt.mediamanager.model.RequestType;
import report.butt.mediamanager.model.TvRequest;

@Component
public class SonarrMonitored implements Validator<TvRequest> {

    private static final String MONITOR_ALL = "all";

    @Override
    public Boolean validate(TvRequest request) {
        return Boolean.TRUE.equals(request.getSonarrMonitored())
                && MONITOR_ALL.equals(request.getSonarrMonitoredAll());
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
        return "Monitored?";
    }

    @Override
    public String description() {
        return "Sonarr is monitoring the series and set to monitor all new items.";
    }
}
