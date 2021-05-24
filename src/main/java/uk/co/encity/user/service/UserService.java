package uk.co.encity.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.util.Logger;
import uk.co.encity.user.commands.ConfirmUserCommand;
import uk.co.encity.user.commands.PatchUserCommand;
import uk.co.encity.user.commands.PreConditionException;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.events.published.UserMessage;
import uk.co.encity.user.events.published.UserMessageSerializer;

import java.io.IOException;

public abstract class UserService {

    public abstract UserRepository getRepository();
    public abstract AmqpTemplate getAmqpTemplate();
    public abstract String getTopicExchangeName();
    public abstract Logger getLogger();
    public abstract ObjectMapper getMapper();

    /**
     * Attempt to perform a state transition command on a User, applying the necessary
     * business logic and saving the event
     * @param command the command (transition) to perform
     * @return the affected User
     */
    public User applyCommand(PatchUserCommand command) throws
            UnsupportedOperationException,
            IllegalArgumentException,
            PreConditionException,
            IOException
    {
        UserRepository userRepo = this.getRepository();

        // Try to get the user (could throw IOException)
        User theUser = userRepo.getUser(command.getUserId());

        if (theUser != null) {
            // Check the pre-conditions of the command - might throw
            command.checkPreConditions(theUser);
        } else {
            throw new IllegalArgumentException(String.format("User with id %s does not exist", command.getUserId()), null);
        }

        // Perform the command
        switch (command.getCmdType()) {
            case CONFIRM_USER:
                userRepo.confirmUser(theUser, ((ConfirmUserCommand)command).getInitialPassword());
                break;
            case REJECT_USER:
                // No special action is needed to reject a user - the event will be saved (below)
                break;
            default:
                throw new UnsupportedOperationException("Command not Supported: " + command.getCmdType().toString(), null);
        }

        // Save an event
        UserEvent evt = command.createUserEvent(theUser);
        userRepo.addUserEvent(command.getCommandId(), evt.getUserEventType(), theUser);

        // Publish the event
        SimpleModule module = new SimpleModule();

        UserMessage outboundMsg = new UserMessage(theUser, evt);
        this.getLogger().debug("Sending message...");
        module.addSerializer(UserMessage.class, new UserMessageSerializer());
        this.getMapper().registerModule(module);
        String jsonEvt;

        jsonEvt = this.getMapper().writeValueAsString(outboundMsg);
        this.getAmqpTemplate().convertAndSend(this.getTopicExchangeName(), evt.getRoutingKey(), jsonEvt);

        return theUser;
    }
}
