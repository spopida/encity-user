package uk.co.encity.user.commands;

import lombok.Getter;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;
import uk.co.encity.user.events.generated.UserDeletedEvent;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.events.generated.UserRejectedEvent;
import uk.co.encity.user.service.UserRepository;

@Getter
public class DeleteUserCommand extends UserCommand{

    public DeleteUserCommand(String userId, UserRepository repo) {
        super(userId, UserTenantCommandType.DELETE_USER, repo);
    }

    @Override
    public void checkPreConditions(User u) throws PreConditionException {
        if (u.getTenantStatus() != UserTenantStatus.UNCONFIRMED) {
            throw new PreConditionException(
                    "Cannot delete user " + u.getUserId() + " due to failed pre-condition");
        }
        if (u.getProviderStatus() != UserProviderStatus.ACTIVE) {
            throw new PreConditionException(
                    "Cannot delete user " + u.getUserId() + " due to failed pre-condition");
        }
        return;
    }

    @Override
    public UserEvent createUserEvent(User u) {
        return new UserDeletedEvent(this.getCommandId(), u, this.getRepo());
    }
}
