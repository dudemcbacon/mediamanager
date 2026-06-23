package report.butt.mediamanager.model.radarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public class RadarrCommand {

    @JsonProperty("id")
    private @Nullable Integer id;

    @JsonProperty("name")
    private @Nullable String name;

    @JsonProperty("commandName")
    private @Nullable String commandName;

    @JsonProperty("priority")
    private @Nullable String priority;

    @JsonProperty("status")
    private @Nullable String status;

    @JsonProperty("result")
    private @Nullable String result;

    @JsonProperty("queued")
    private @Nullable String queued;

    @JsonProperty("started")
    private @Nullable String started;

    @JsonProperty("trigger")
    private @Nullable String trigger;

    @JsonProperty("stateChangeTime")
    private @Nullable String stateChangeTime;

    public @Nullable Integer getId() {
        return id;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public @Nullable String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public @Nullable String getCommandName() {
        return commandName;
    }

    public void setCommandName(@Nullable String commandName) {
        this.commandName = commandName;
    }

    public @Nullable String getPriority() {
        return priority;
    }

    public void setPriority(@Nullable String priority) {
        this.priority = priority;
    }

    public @Nullable String getStatus() {
        return status;
    }

    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    public @Nullable String getResult() {
        return result;
    }

    public void setResult(@Nullable String result) {
        this.result = result;
    }

    public @Nullable String getQueued() {
        return queued;
    }

    public void setQueued(@Nullable String queued) {
        this.queued = queued;
    }

    public @Nullable String getStarted() {
        return started;
    }

    public void setStarted(@Nullable String started) {
        this.started = started;
    }

    public @Nullable String getTrigger() {
        return trigger;
    }

    public void setTrigger(@Nullable String trigger) {
        this.trigger = trigger;
    }

    public @Nullable String getStateChangeTime() {
        return stateChangeTime;
    }

    public void setStateChangeTime(@Nullable String stateChangeTime) {
        this.stateChangeTime = stateChangeTime;
    }
}
