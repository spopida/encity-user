package uk.co.encity.user.repositories.mongodb;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.UserEvent;
import uk.co.encity.user.events.UserEventType;

import java.time.Instant;

@SuperBuilder
@Getter
@BsonDiscriminator
// TODO: ??? REVISIT - should it implement UserEvent - it didn't yesterday!
public abstract class MongoDBUserEvent {
    @BsonProperty("_id")
    private ObjectId eventId;
    private ObjectId userId;
    private Instant eventTime;
    private int userVersionNumber;
    private Instant expiryTime;

    public MongoDBUserEvent() {
        this.eventId = new ObjectId();
    }

    /**
     * Apply this event to a given snapshot, producing a new snapshot
     * @param snap
     * @return
     */
    protected abstract UserSnapshot applyToUserSnapshot(final UserSnapshot snap);

    protected final UserSnapshot updateUserSnapshot(final UserSnapshot snap) {
        // Update the core fields
        UserSnapshot snapCopy = new UserSnapshot(snap);
        snapCopy.setLastUpdate(this.eventTime);
        snapCopy.setToVersion(this.userVersionNumber);
        snapCopy.setExpiryTime(this.expiryTime);
        return this.applyToUserSnapshot(snapCopy);
    }
}
