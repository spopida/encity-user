package uk.co.encity.user.commands;

import uk.co.encity.user.entity.User;
import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;
import uk.co.encity.user.events.generated.UserConfirmedEvent;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.service.UserRepository;

import java.util.Map;

public class ConfirmUserCommand extends PatchUserCommand {

    public enum Extras {
        INITIAL_PASSWORD
    }

    transient private String initialPassword;

    public ConfirmUserCommand(String userId, UserRepository repo, Map extras) {
        super(UserTenantCommandType.CONFIRM_USER, userId, repo);
        this.initialPassword = (String)extras.get(Extras.INITIAL_PASSWORD);
    }

    public String getInitialPassword() { return this.initialPassword; }

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
}
