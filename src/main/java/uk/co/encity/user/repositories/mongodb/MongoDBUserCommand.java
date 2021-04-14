package uk.co.encity.user.repositories.mongodb;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import uk.co.encity.user.commands.UserCommand;

import java.time.Instant;

@BsonDiscriminator
@Getter
@Setter
@SuperBuilder
public abstract class MongoDBUserCommand {

    @BsonProperty("_id")
    private ObjectId commandId;
    private Instant timeStamp;
    private UserCommand.UserTenantCommandType commandType;

    public MongoDBUserCommand() {
        this.commandId = new ObjectId();
        this.timeStamp = Instant.now();
    }

    @BsonProperty("_id") public ObjectId getCommandId() { return this.commandId; }
}
