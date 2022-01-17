package uk.co.encity.user.repositories.mongodb;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.events.generated.UserEventType;
import uk.co.encity.user.service.UserRepository;

import java.time.Instant;

@SuperBuilder
@Getter
@Setter
@BsonDiscriminator
public abstract class MongoDBUserEvent {
    @BsonProperty("_id")
    private ObjectId eventId;
    private ObjectId userId;
    private ObjectId commandId;
    private Instant eventTime;
    private int userVersionNumber;
    private UserEventType userEventType;

    /**
     * Default constructor for creation of an existing event prior to calling setters
     */
    public MongoDBUserEvent() {}

    /**
     * Apply this event to a given snapshot, producing a new snapshot
     * @param snap
     * @return
     */
    protected abstract UserSnapshot applyToUserSnapshot(final UserSnapshot snap);

    protected final UserSnapshot updateUser(final UserSnapshot userSnap) {
        // Update the core fields
        userSnap.setLastUpdate(this.eventTime);
        userSnap.setToVersion(this.userVersionNumber);

        return this.applyToUserSnapshot(userSnap);
    }

    protected abstract UserEvent asUserEvent(String commandId, User user, UserRepository repo);
}
