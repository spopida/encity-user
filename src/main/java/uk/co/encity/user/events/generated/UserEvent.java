package uk.co.encity.user.events.generated;

import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Getter;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.service.UserRepository;

import java.time.Instant;

@Getter
public abstract class UserEvent {

    // TODO: Missing version number ??
    private String commandId;
    private String eventId;
    private String userId;
    private Instant eventTime;

    public UserEvent(String commandId, User user, UserRepository repo) {
        this.commandId = commandId;
        this.eventId = repo.getIdentity();
        this.userId = user.getUserId();
        this.eventTime = Instant.now();
    }

    abstract public UserEventType getUserEventType();
    abstract public String getRoutingKey();

}
