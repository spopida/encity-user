package uk.co.encity.user.events.published;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import uk.co.encity.user.events.generated.UserCreatedEvent;

import java.io.IOException;

public class UserCreatedMessageSerializer extends StdSerializer<UserCreatedMessage> {
    public UserCreatedMessageSerializer() {
        this(null);
    }

    public UserCreatedMessageSerializer(Class<UserCreatedMessage> m) {
        super(m);
    }

    @Override
    public void serialize(UserCreatedMessage value, JsonGenerator jGen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jGen.writeStartObject();
        jGen.writeObjectField("user", value.getUser());
        jGen.writeObjectField("event", value.getEvent());
        jGen.writeEndObject();
    }
}
