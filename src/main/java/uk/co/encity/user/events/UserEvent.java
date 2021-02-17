package uk.co.encity.user.events;

import uk.co.encity.user.components.HasUser;

import java.time.Instant;

public interface UserEvent extends HasUser {
    public String getEventId();
    public Instant getEventTime();
    public UserEventType getUserEventType();
}
