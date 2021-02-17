package uk.co.encity.user.repositories.mongodb;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.Instant;

@SuperBuilder
@Getter
@BsonDiscriminator
public class MongoDBUserEvent {
    @BsonProperty("_id")
    private ObjectId eventId;
    private ObjectId userId;
    private Instant eventTime;
    private int userVersionNumber;

    public MongoDBUserEvent() {
        this.eventId = new ObjectId();
    }
}
