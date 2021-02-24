package uk.co.encity.user.repositories.mongodb;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import uk.co.encity.user.commands.RejectUserCommand;

@BsonDiscriminator
public class MongoDBRejectUserCommand extends MongoDBPatchUserCommand {
    public MongoDBRejectUserCommand(RejectUserCommand cmd) {
        super(cmd);
    }
}
