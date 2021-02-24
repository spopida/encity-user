package uk.co.encity.user.commands;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.co.encity.user.service.UserRepository;

import java.io.IOException;

public class PatchUserCommandDeserializer extends StdDeserializer<PatchUserCommand> {

    private String userId;
    private UserRepository repo;

    public PatchUserCommandDeserializer(String userId, UserRepository repo) {
        this(null, userId, repo);
    }

    public PatchUserCommandDeserializer(Class<?> valueClass, String userId, UserRepository repo) {
        super(valueClass);
        this.userId = userId;
        this.repo = repo;
    }

    @Override
    public PatchUserCommand deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String transition = node.get("action").asText();

        return PatchUserCommand.getPatchUserCommand(
            UserCommand.ACTION_MAP.get(transition),
            this.userId,
            this.repo
        );
    }
}
