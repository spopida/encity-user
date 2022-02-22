package uk.co.encity.user.commands;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.co.encity.user.service.UserRepository;

import java.io.IOException;

public class CreateUserCommandDeserializer extends StdDeserializer<CreateUserCommand> {

    private UserRepository repo;

    public CreateUserCommandDeserializer(Class<?> valueClass, UserRepository repo) {
        super(valueClass);
        this.repo = repo;
    }

    @Override
    public CreateUserCommand deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        String firstName = node.get("firstname").asText();
        String lastName = node.get("lastname").asText();
        String email = node.get("email").asText();
        String domain = node.get("domain").asText();
        Boolean isAdmin = node.get("isAdmin").asBoolean();
        String id = node.get("id").asText();

        return new CreateUserCommand(repo, firstName, lastName, email, domain, isAdmin, id);
    }
}
