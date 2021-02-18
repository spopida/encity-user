package uk.co.encity.user.events;

import lombok.Builder;
import lombok.Getter;
import reactor.util.Logger;
import reactor.util.Loggers;
import uk.co.encity.user.components.EmailRecipient;

import java.time.Instant;

/**
 * An immutable event representing confirmation of a tenancy.  The class is constructed using
 * the builder pattern, avoiding ugly long parameter lists, but still ensuring immutability (no setters)
 */
@Builder @Getter
public class TenancyConfirmedEvent {
    private final Logger logger = Loggers.getLogger(getClass());

    private String eventId;
    private TenancyEventType eventType;
    private String tenancyId;
    private String commandId;
    private Instant eventDateTime;
    private int tenancyVersionNumber;
    private EmailRecipient adminUser;
    private final String domain;
}
