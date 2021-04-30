package uk.co.encity.user.commands;

import uk.co.encity.user.entity.User;
import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;
import uk.co.encity.user.events.generated.UserConfirmedEvent;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.service.UserRepository;

public class ConfirmUserCommand extends PatchUserCommand {

    public ConfirmUserCommand(String userId, UserRepository repo) {
        super(UserTenantCommandType.CONFIRM_USER, userId, repo);
    }

    @Override
    public void checkPreConditions(User u) throws PreConditionException {
        if (u.getTenantStatus() != UserTenantStatus.UNCONFIRMED) {
            throw new PreConditionException(
                "UserTenantStatus should be UNCONFIRMED");
        }
        if (u.getProviderStatus() != UserProviderStatus.ACTIVE) {
            throw new PreConditionException(
                "UserProviderStatus should be ACTIVE");
        }
        return;
    }

    @Override
    public UserEvent createUserEvent(User u) {
        return new UserConfirmedEvent(this.getCommandId(), u, this.getRepo());
    }

    @Override
    public User perform(User target) {
        throw new UnsupportedOperationException("Not implemented");

        // Call the Auth0 management API to create the user
    }
}
