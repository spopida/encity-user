package uk.co.encity.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.Logger;
import reactor.util.Loggers;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.consumed.TenancyConfirmedEvent;
import uk.co.encity.user.events.consumed.TenancyConfirmedEventDeserializer;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.events.generated.UserEventType;
import uk.co.encity.user.events.published.UserCreatedMessage;
import uk.co.encity.user.events.published.UserCreatedMessageSerializer;
import uk.co.encity.user.events.published.UserMessage;

import java.io.IOException;

/**
 * Handle an event confirming the creation of a new tenancy by setting up
 * an Admin User for the tenancy
 */
@Component
public class TenancyConfirmedHandler {

    /**
     * The name of the AMQP exchange used for message publication
     */
    private static final String topicExchangeName = "encity-exchange";

    /**
     * The {@link Logger} for this class
     */
    private static final Logger logger = Loggers.getLogger(TenancyConfirmedHandler.class);

    private final UserRepository userRepo;

    /**
     * The RabbitMQ helper class
     */
    private final RabbitTemplate rabbitTemplate;

    public TenancyConfirmedHandler(@Autowired UserRepository repo, @Autowired RabbitTemplate rabbitTmpl) {
        this.userRepo = repo;
        this.rabbitTemplate = rabbitTmpl;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }

    public void receiveMessage(String message) {
        logger.debug("Received <" + message + ">");

        // De-serialise the JSON into a POJO
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TenancyConfirmedEvent.class, new TenancyConfirmedEventDeserializer());
        mapper.registerModule(module);
        mapper.registerModule(new JavaTimeModule()); // Needed for handling java.time.Instant objects

        TenancyConfirmedEvent evt = null;
        try {
            evt = mapper.readValue(message, TenancyConfirmedEvent.class);
            logger.debug("Tenancy confirmed event de-serialised successfully");
        } catch (IOException e) {
            logger.error("Error de-serialising tenancy confirmed event: " + e.getMessage(), e);
        }

        try {
            // Generate a UserCreatedEvent and publish it
            User theUser = this.userRepo.addUser(evt.getTenancyId(), evt.getDomain(), evt.getAdminUser(), true);
            UserEvent userEvent = this.userRepo.addUserEvent(evt.getCommandId(), UserEventType.USER_CREATED, theUser);
            // TODO: delegate the above to the UserService?

            UserMessage outboundMsg = new UserMessage(theUser, userEvent);
            logger.debug("Sending message...");
            module.addSerializer(UserCreatedMessage.class, new UserCreatedMessageSerializer());
            mapper.registerModule(module);
            String jsonEvt;
            try {
                jsonEvt = mapper.writeValueAsString(outboundMsg);
                rabbitTemplate.convertAndSend(topicExchangeName, "encity.user.created", jsonEvt);
            } catch (IOException e) {
                logger.error("Error publishing user created message: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            logger.error("Error adding user to repository" + e.getMessage(), e);
        }
    }
}
