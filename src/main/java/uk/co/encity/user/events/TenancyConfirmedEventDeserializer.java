package uk.co.encity.user.events;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.co.encity.user.components.EmailRecipient;

import java.io.IOException;
import java.time.Instant;

/**
 * A classed used for de-serializing events from the event bus, representing
 * confirmation of a new tenancy
 */
public class TenancyConfirmedEventDeserializer extends StdDeserializer<TenancyConfirmedEvent> {
    public TenancyConfirmedEventDeserializer() {
        this(null);
    }

    public TenancyConfirmedEventDeserializer(Class<?> valueClass) {
        super(valueClass);
    }

    @Override
    public TenancyConfirmedEvent deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        String eventId = node.get("eventId").asText();
        TenancyEventType eventType = TenancyEventType.valueOf(node.get("eventType").asText());
        String tenancyId = node.get("tenancyId").asText();
        //String commandId = node.get("commandId").asText();

        JsonNode edtNode = node.get("eventDateTime");
        long sec = edtNode.get("epochSecond").asLong();
        long nan = edtNode.get("nano").asLong();
        Instant eventDateTime = Instant.ofEpochSecond(sec, nan);

        EmailRecipient userContact = this.deserializeContact(node.get("originalAdminUser"));
        //int tenancyVersionNumber = node.get("tenancyVersionNumber").asInt();

        String domain = node.get("domain").asText();

        return
            TenancyConfirmedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .tenancyId(tenancyId)
                //.commandId(commandId)
                .eventDateTime(eventDateTime)
                //.tenancyVersionNumber(tenancyVersionNumber)
                .adminUser(userContact)
                .domain(domain)
            .build();
    }

    private EmailRecipient deserializeContact(JsonNode node) {
        String first = node.get("firstName").asText();
        String last = node.get("lastName").asText();
        String email = node.get("emailAddress").asText();

        return new EmailRecipient(first, last, email);
    }
}
