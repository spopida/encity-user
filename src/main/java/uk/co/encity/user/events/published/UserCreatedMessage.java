package uk.co.encity.user.events.published;

import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.generated.UserEvent;

public class UserCreatedMessage extends UserMessage {

    public UserCreatedMessage(User user, UserEvent event) { super(user, event); }
}
