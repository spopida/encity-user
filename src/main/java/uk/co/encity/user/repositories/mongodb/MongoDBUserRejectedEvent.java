package uk.co.encity.user.repositories.mongodb;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.entity.UserTenantStatus;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.events.generated.UserRejectedEvent;
import uk.co.encity.user.service.UserRepository;

@SuperBuilder
@Getter
@BsonDiscriminator
public class MongoDBUserRejectedEvent extends MongoDBUserEvent {

    public MongoDBUserRejectedEvent() {
        super();
    }

    @Override
    protected UserSnapshot applyToUserSnapshot(UserSnapshot snap) {
        snap.setTenantStatus(UserTenantStatus.REJECTED);
        return snap;
    }

    @Override
    protected UserEvent asUserEvent(String commandId, User user, UserRepository repo) {
        return new UserRejectedEvent(commandId, user, repo);
    }
}
