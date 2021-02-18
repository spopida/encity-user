package uk.co.encity.user.repositories.mongodb;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import uk.co.encity.user.events.UserEventType;

@SuperBuilder
@Getter
@BsonDiscriminator
public class MongoDBUserCreatedEvent extends MongoDBUserEvent {
    private UserEventType userEventType;

    public MongoDBUserCreatedEvent() {
        super();
    }

    @Override
    protected UserSnapshot applyToUserSnapshot(UserSnapshot snap) {
        // Nothing to do - the snap should be up to date
        return snap;
    }
}
