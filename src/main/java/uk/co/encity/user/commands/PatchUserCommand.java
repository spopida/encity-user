package uk.co.encity.user.commands;

import lombok.Getter;
import org.springframework.lang.NonNull;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.service.UserRepository;

import java.util.Map;

@Getter
public abstract class PatchUserCommand extends UserCommand {

    public PatchUserCommand(UserCommand.UserTenantCommandType cmdType, String userId, UserRepository repo) {
        super(userId, cmdType, repo);
    }

    public static PatchUserCommand getPatchUserCommand(
            @NonNull UserTenantCommandType cmdtype,
            String userId,
            UserRepository repo,
            Map extras)
    {
        PatchUserCommand patchCmd = null;

        switch (cmdtype) {
            case CONFIRM_USER:
                patchCmd = new ConfirmUserCommand(userId, repo, extras);
                break;
            case REJECT_USER:
                patchCmd = new RejectUserCommand(userId, repo);
                break;
        }

        return patchCmd;
    }
}
