package uk.co.encity.user.events.generated;

import uk.co.encity.user.entity.User;
import uk.co.encity.user.service.UserRepository;

import java.time.Instant;

public class UserCreatedEvent extends UserEvent {

    private Instant expiryTime;

    public UserCreatedEvent(String commandId, User user, UserRepository repo, Instant expiry) {
        super(commandId, user, repo);
        this.expiryTime = expiry;
    }

    @Override
    public UserEventType getUserEventType() {
        return UserEventType.USER_CREATED;
    }

    @Override
    public String getRoutingKey() {
        return "encity.user.created";
    }


}
