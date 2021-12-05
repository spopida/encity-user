package uk.co.encity.user.service;

import uk.co.encity.user.commands.PatchUserCommand;
import uk.co.encity.user.commands.UserCommand;
import uk.co.encity.user.components.EmailRecipient;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.events.generated.UserEventType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface UserRepository {

    public String getIdentity();
    public User getUser(String userId) throws IOException;

    /**
     * Get a list of users associated with a given tenancy
     * @param tenancyId the identity of the tenancy to which the users belong
     * @return a list of associated users
     */
    public List<User> getTenancyUsers(String tenancyId);
    public User confirmUser(User user, String initialPassword) throws IOException;
    public User addUser(String tenancyId, String domain, EmailRecipient user, boolean isAdmin) throws IOException;
    public PatchUserCommand addPatchUserCommand(UserCommand.UserTenantCommandType type, PatchUserCommand cmd);
    public UserEvent addUserEvent(String commandId, UserEventType type, User user);
}
