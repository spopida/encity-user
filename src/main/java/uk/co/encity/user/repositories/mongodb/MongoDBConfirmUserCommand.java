package uk.co.encity.user.repositories.mongodb;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import uk.co.encity.user.commands.ConfirmUserCommand;

@BsonDiscriminator
public class MongoDBConfirmUserCommand extends MongoDBPatchUserCommand {
    public MongoDBConfirmUserCommand(ConfirmUserCommand cmd) {
        super(cmd);
    }
}
