package uk.co.encity.user.events.generated;

import uk.co.encity.user.commands.UserCommand;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.service.UserRepository;

public class UserConfirmedEvent extends UserEvent {

    private String commandId;

    public UserConfirmedEvent(String commandId, User u, UserRepository repo) {
        super(commandId, u, repo);
        this.commandId = commandId;
    }

    public UserEventType getUserEventType() { return UserEventType.USER_CONFIRMED; }

    @Override
    public String getRoutingKey() {
        return "encity.user.confirmed";
    }
}
