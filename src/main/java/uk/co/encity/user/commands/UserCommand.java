package uk.co.encity.user.commands;

//import org.bson.codecs.pojo.annotations.BsonProperty;
//import org.bson.types.ObjectId;

import lombok.Getter;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.service.UserRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class UserCommand {

    // Defines the commands that a Tenant may perform on a user
    public enum UserTenantCommandType {
        DEMOTE_USER,
        PROMOTE_USER,
        UPDATE_USER,
        REVOKE_USER,
        RESTORE_USER,
        CONFIRM_USER,
        REJECT_USER,
        RESET_USER,
        DELETE_USER,
        CREATE_USER
    }

    protected static final Map<String, UserTenantCommandType> ACTION_MAP;

    static {
        ACTION_MAP = new HashMap<String, UserTenantCommandType>();
        ACTION_MAP.put("confirm", UserTenantCommandType.valueOf("CONFIRM_USER"));
        ACTION_MAP.put("reject", UserTenantCommandType.valueOf("REJECT_USER"));
    }

    // Defines the commands that the Provider may perform on a user
    public enum UserProviderCommandType {
        SUSPEND_USER,
        RELEASE_USER,
        STOP_USER
    }

    private String userId;
    private String commandId;
    private Instant timeStamp;
    private UserCommand.UserTenantCommandType cmdType;
    private UserRepository repo;

    public UserCommand(String userId, UserCommand.UserTenantCommandType cmdType, UserRepository repo) {
        this.commandId = repo.getIdentity();
        this.timeStamp = Instant.now();
        this.cmdType = cmdType;
        this.repo = repo;
        this.userId = userId;
    }

    public abstract void checkPreConditions(User u) throws PreConditionException;
    public abstract UserEvent createUserEvent(User u);
}
