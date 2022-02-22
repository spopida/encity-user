package uk.co.encity.user.commands;

import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.service.UserRepository;

public class CreateUserCommand extends UserCommand {

    public CreateUserCommand(UserRepository repo, String firstName, String lastName, String email, String domain, Boolean isAdmin, String id) {
        // TODO: Refactor UserCommand to allow for commands without a userId
        super("", UserTenantCommandType.CREATE_USER, repo);
    }

    @Override
    public void checkPreConditions(User u) throws PreConditionException {
        return;
    }

    @Override
    public UserEvent createUserEvent(User u) {
        return null;
    }
}
