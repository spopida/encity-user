package uk.co.encity.user.events.published;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class UserMessageSerializer extends StdSerializer<UserMessage> {
    public UserMessageSerializer() {
        this(null);
    }

    public UserMessageSerializer(Class<UserMessage> m) {
        super(m);
    }

    @Override
    public void serialize(UserMessage value, JsonGenerator jGen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jGen.writeStartObject();
        jGen.writeObjectField("user", value.getUser());
        jGen.writeObjectField("event", value.getEvent());
        jGen.writeEndObject();
    }
}
