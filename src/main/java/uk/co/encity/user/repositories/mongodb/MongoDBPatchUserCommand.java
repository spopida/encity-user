package uk.co.encity.user.repositories.mongodb;

import lombok.Getter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.types.ObjectId;
import uk.co.encity.user.commands.ConfirmUserCommand;
import uk.co.encity.user.commands.PatchUserCommand;
import uk.co.encity.user.commands.RejectUserCommand;

@BsonDiscriminator
@Getter
public class MongoDBPatchUserCommand extends MongoDBUserCommand {
    private ObjectId userId;

    public MongoDBPatchUserCommand(PatchUserCommand cmd) {
        super();
        this.userId = new ObjectId(cmd.getUserId());
    }

    public static MongoDBPatchUserCommand getMongoDBPatchUserCommand(PatchUserCommand cmd) {
        MongoDBPatchUserCommand dbCmd = null;
        switch (cmd.getCmdType()) {
            case CONFIRM_USER:
                dbCmd = new MongoDBConfirmUserCommand((ConfirmUserCommand)cmd);
                break;
            case REJECT_USER:
                dbCmd = new MongoDBRejectUserCommand((RejectUserCommand)cmd);
                break;
            default:
                throw new IllegalArgumentException("Invalid command type");
        }
        return dbCmd;
    }
}
