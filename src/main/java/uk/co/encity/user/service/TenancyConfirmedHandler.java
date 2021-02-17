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
import uk.co.encity.user.events.*;
import uk.co.encity.user.repositories.mongodb.MongoDBUserRepository;
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
            logger.error("Error de-serialising tenancy confirmed event: " + e.getMessage());
        }

        User theUser = this.userRepo.addUser(evt.getTenancyId(), evt.getAdminUser(), true);
        UserEvent userEvent = this.userRepo.addUserEvent(UserEventType.USER_CREATED, theUser);

        // Generate a UserCreatedEvent and publish it
        logger.debug("Sending message...");
        module.addSerializer(UserCreatedEvent.class, new UserCreatedEventSerializer());
        mapper.registerModule(module);
        String jsonEvt;
        try {
            jsonEvt = mapper.writeValueAsString(userEvent);
            rabbitTemplate.convertAndSend(topicExchangeName, "encity.user.created", jsonEvt);
        } catch (IOException e) {
            logger.error("Error publishing user created event: " + e.getMessage());
            // But carry on attempting to generate a response to the client
        }

        // After that create a separate controller to get and receive patch requests...then write a UI to allow the user
        // to do this...then look into how to use Auth0
    }
}
