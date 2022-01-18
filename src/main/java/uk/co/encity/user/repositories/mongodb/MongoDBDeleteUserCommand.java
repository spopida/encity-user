package uk.co.encity.user.repositories.mongodb;

import lombok.Getter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.types.ObjectId;
import uk.co.encity.user.commands.DeleteUserCommand;

@BsonDiscriminator
@Getter
public class MongoDBDeleteUserCommand extends MongoDBUserCommand{

    private ObjectId userId;

    public MongoDBDeleteUserCommand(DeleteUserCommand cmd) {
        super();
        this.userId = new ObjectId(cmd.getUserId());
    }
}
