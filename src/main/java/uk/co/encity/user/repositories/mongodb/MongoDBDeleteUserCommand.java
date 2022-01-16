package uk.co.encity.user.repositories.mongodb;

import org.bson.types.ObjectId;
import uk.co.encity.user.commands.DeleteUserCommand;

public class MongoDBDeleteUserCommand extends MongoDBUserCommand{

    private ObjectId userId;

    public MongoDBDeleteUserCommand(DeleteUserCommand cmd) {
        super();
        this.userId = new ObjectId(cmd.getUserId());
    }
}
