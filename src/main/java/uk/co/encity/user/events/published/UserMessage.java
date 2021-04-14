package uk.co.encity.user.events.published;

import lombok.Getter;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.generated.UserEvent;

@Getter
public class UserMessage {
    private User user;
    private UserEvent event;

    public UserMessage(User user, UserEvent event) {
        this.user = user;
        this.event = event;
    }
}
