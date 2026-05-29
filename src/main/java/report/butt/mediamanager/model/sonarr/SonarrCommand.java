package report.butt.mediamanager.model.sonarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarrCommand {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("commandName")
    private String commandName;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("status")
    private String status;

    @JsonProperty("result")
    private String result;

    @JsonProperty("queued")
    private String queued;

    @JsonProperty("started")
    private String started;

    @JsonProperty("trigger")
    private String trigger;

    @JsonProperty("stateChangeTime")
    private String stateChangeTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getQueued() {
        return queued;
    }

    public void setQueued(String queued) {
        this.queued = queued;
    }

    public String getStarted() {
        return started;
    }

    public void setStarted(String started) {
        this.started = started;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getStateChangeTime() {
        return stateChangeTime;
    }

    public void setStateChangeTime(String stateChangeTime) {
        this.stateChangeTime = stateChangeTime;
    }
}
