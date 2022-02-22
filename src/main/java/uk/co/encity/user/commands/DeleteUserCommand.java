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
        if (u.getTenantStatus() != UserTenantStatus.CONFIRMED) {
            throw new PreConditionException(
                    "Cannot delete user " + u.getUserId() + " as it is not confirmed");
        }
        if (u.getProviderStatus() != UserProviderStatus.ACTIVE) {
            throw new PreConditionException(
                    "Cannot delete user " + u.getUserId() + " as it is not active");
        }
        // If the subject user is and Admin user and is the only one, then it can't be deleted (as this would put
        // the tenancy in a state such that it could not be properly administered and it could not recover from this
        // state
        if (u.isAdminUser() && this.getRepo().getAdminUserCount(u.getTenancyId()) == 1) {
            throw new PreConditionException(
                    "Cannot delete user " + u.getUserId() + " as it is the only Admin user for its tenancy");
        }
        return;
    }

    @Override
    public UserEvent createUserEvent(User u) {
        return new UserDeletedEvent(this.getCommandId(), u, this.getRepo());
    }
}
