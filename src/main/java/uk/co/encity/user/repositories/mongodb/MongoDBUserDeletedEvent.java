package uk.co.encity.user.repositories.mongodb;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.entity.UserTenantStatus;
import uk.co.encity.user.events.generated.UserDeletedEvent;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.service.UserRepository;

@SuperBuilder
@Getter
@BsonDiscriminator
public class MongoDBUserDeletedEvent extends MongoDBUserEvent{

    public MongoDBUserDeletedEvent() {
        super();
    }

    @Override
    protected UserSnapshot applyToUserSnapshot(UserSnapshot snap) {
        snap.setTenantStatus(UserTenantStatus.DELETED);
        return snap;
    }

    @Override
    protected UserEvent asUserEvent(String commandId, User user, UserRepository repo) {
        return new UserDeletedEvent(commandId, user, repo);
    }
}
