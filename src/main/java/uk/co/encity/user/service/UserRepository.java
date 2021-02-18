package uk.co.encity.user.service;

import uk.co.encity.user.components.EmailRecipient;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.UserEvent;
import uk.co.encity.user.events.UserEventType;

import java.io.IOException;

public interface UserRepository {

    public User getUser(String userId) throws IOException;
    public User addUser(String tenancyId, String domain, EmailRecipient user, boolean isAdmin);
    public UserEvent addUserEvent(UserEventType type, User user);
}
