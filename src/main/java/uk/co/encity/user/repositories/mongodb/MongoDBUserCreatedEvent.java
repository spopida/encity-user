package uk.co.encity.user.repositories.mongodb;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.events.generated.UserCreatedEvent;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.service.UserRepository;

import java.time.Instant;

@SuperBuilder
@Getter
@BsonDiscriminator
public class MongoDBUserCreatedEvent extends MongoDBUserEvent {
    private Instant expiryTime;

    public MongoDBUserCreatedEvent() {
        super();
    }

    @Override
    protected UserSnapshot applyToUserSnapshot(UserSnapshot snap) {
        // Nothing to do - the snap should be up to date
        snap.setExpiryTime(this.expiryTime);
        return snap;
    }

    @Override
    protected UserEvent asUserEvent(String commandId, User user, UserRepository repo) {
        return new UserCreatedEvent(commandId, user, repo, this.expiryTime);
    }
}
