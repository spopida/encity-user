package uk.co.encity.user.service.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import reactor.util.Logger;
import reactor.util.Loggers;
import uk.co.encity.user.service.UserRepository;
import uk.co.encity.user.service.UserService;

@Service
@Getter
public class JacksonUserService extends UserService {

    /**
     * The {@link Logger} for this class
     */
    private final Logger logger = Loggers.getLogger(getClass());

    /**
     * The repository of users
     */
    private final UserRepository repository;

    /**
     * The RabbitMQ helper class
     */
    private final AmqpTemplate amqpTemplate;

    /**
     * The name of the AMQP exchange used for message publication
     */
    private final String topicExchangeName = "encity-exchange";

    private final ObjectMapper mapper;

    public JacksonUserService(UserRepository repo, AmqpTemplate rabbitTmpl, ObjectMapper mapper) {
        this.repository = repo;
        this.amqpTemplate = rabbitTmpl;
        this.mapper = mapper;
    }

}
