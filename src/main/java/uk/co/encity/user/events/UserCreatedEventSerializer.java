package uk.co.encity.user.events;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class UserCreatedEventSerializer extends StdSerializer<UserCreatedEvent> {
    public UserCreatedEventSerializer() {
        this(null);
    }

    public UserCreatedEventSerializer(Class<UserCreatedEvent> t) {
        super(t);
    }

    @Override
    public void serialize(UserCreatedEvent value, JsonGenerator jGen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jGen.writeStartObject();
        jGen.writeFieldName("userId");
        jGen.writeString(value.getUserId());
        jGen.writeFieldName("tenancyId");
        jGen.writeString(value.getTenancyId());
        jGen.writeFieldName("firstName");
        jGen.writeString(value.getFirstName());
        jGen.writeFieldName("lastName");
        jGen.writeString(value.getLastName());
        jGen.writeFieldName("emailAddress");
        jGen.writeString(value.getEmailAddress());
        jGen.writeFieldName("isAdminUser");
        jGen.writeBoolean(value.isAdminUser());
        jGen.writeFieldName("userVersionNumber");
        jGen.writeNumber(value.getVersion());
        jGen.writeFieldName("lastUpdate");
        jGen.writeObject(value.getLastUpdate());
        jGen.writeFieldName("tenantStatus");
        jGen.writeString(value.getTenantStatus().toString());
        jGen.writeFieldName("providerStatus");
        jGen.writeString(value.getProviderStatus().toString());
        jGen.writeFieldName("eventTime");
        jGen.writeObject(value.getEventTime());
        jGen.writeFieldName("eventType");
        jGen.writeString(value.getUserEventType().toString());
        jGen.writeEndObject();
    }
}
