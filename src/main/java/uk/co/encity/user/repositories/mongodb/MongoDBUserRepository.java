package uk.co.encity.user.repositories.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.util.Logger;
import reactor.util.Loggers;
import uk.co.encity.user.components.EmailRecipient;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;
import uk.co.encity.user.events.UserCreatedEvent;
import uk.co.encity.user.events.UserEvent;
import uk.co.encity.user.events.UserEventType;
import uk.co.encity.user.service.UserRepository;

import java.time.Instant;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Component
public class MongoDBUserRepository implements UserRepository {

    /**
     * The {@link Logger} for this class
     */
    private final Logger logger = Loggers.getLogger(getClass());

    private final MongoClient mongoClient;
    private final MongoDatabase db;
    private final CodecRegistry codecRegistry;

    public MongoDBUserRepository(@Value("${mongodb.uri}") String mongodbURI, @Value("${user.db}") String dbName) {
        ConnectionString connectionString = new ConnectionString(mongodbURI);

        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());

        this.codecRegistry = fromRegistries(
                CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
                MongoClientSettings.getDefaultCodecRegistry(),
                pojoCodecRegistry);

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(this.codecRegistry)
                .build();

        this.mongoClient = MongoClients.create(clientSettings);
        this.db = this.mongoClient.getDatabase(dbName);
    }
/*
    @Override
    public void captureUserEvent(UserEvent event) {
        MongoCollection<UserEvent> events = db.getCollection("user_events", UserEvent.class);
        events.insertOne(event);

    }
*/
    @Override
    public User getUser(String userId) {
        User u = null;
        // Get the latest snapshot, then inflate it with all events since

        return u;
    }

    @Override
    public User addUser(String tenancyId, EmailRecipient user, boolean isAdmin) {
        // Create a snapshot
        UserSnapshot snap = new UserSnapshot();
        snap.setUserId(new ObjectId());
        snap.setTenancyId(new ObjectId(tenancyId));
        snap.setFirstName(user.getFirstName());
        snap.setLastName(user.getLastName());
        snap.setEmailAddress(user.getEmailAddress());
        snap.setAdminUser(isAdmin);
        snap.setTenantStatus(UserTenantStatus.UNCONFIRMED);
        snap.setProviderStatus(UserProviderStatus.ACTIVE);
        snap.setFromVersion(1);
        snap.setToVersion(1);
        snap.setLastUpdate(Instant.now());

        // Store the snapshot
        MongoCollection<UserSnapshot> userSnapshots = db.getCollection("user_snapshots", UserSnapshot.class);
        userSnapshots.insertOne(snap);

        // Return the user
        return new User() {
            public String getUserId() { return snap.getUserId().toHexString(); }
            public String getTenancyId() { return snap.getTenancyId().toHexString(); }
            public String getFirstName() { return snap.getFirstName(); }
            public String getLastName() { return snap.getLastName(); }
            public String getEmailAddress() { return snap.getEmailAddress(); }
            public boolean isAdminUser() { return snap.isAdminUser(); }
            public int getVersion() { return snap.getToVersion(); }
            public Instant getLastUpdate() { return snap.getLastUpdate(); }
            public UserTenantStatus getTenantStatus() { return snap.getTenantStatus(); }
            public UserProviderStatus getProviderStatus() { return snap.getProviderStatus(); }
        };

    }

    @Override
    public UserEvent addUserEvent(UserEventType type, User user) {
        final MongoDBUserEvent evt;
        switch (type) {
            case USER_CREATED:
                evt = MongoDBUserCreatedEvent.builder()
                        .userId(new ObjectId(user.getUserId()))
                        .eventTime(Instant.now())
                        .userVersionNumber(user.getVersion())
                        .eventType(type)
                        .build();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }

        MongoCollection<MongoDBUserEvent> events = db.getCollection("user_events", MongoDBUserEvent.class);
        events.insertOne(evt);

        return new UserCreatedEvent() {
            public String getUserId() { return user.getUserId(); }
            public String getTenancyId() { return user.getTenancyId();}
            public String getFirstName() { return user.getFirstName(); }
            public String getLastName() { return user.getLastName(); }
            public String getEmailAddress() { return user.getEmailAddress(); }
            public UserTenantStatus getTenantStatus() { return user.getTenantStatus(); }
            public UserProviderStatus getProviderStatus() { return user.getProviderStatus(); }
            public boolean isAdminUser() { return user.isAdminUser(); }
            public Instant getLastUpdate() { return user.getLastUpdate(); }
            public int getVersion() { return user.getVersion(); }

            public String getEventId() { return evt.getEventId().toHexString(); }
            public UserEventType getUserEventType() { return UserEventType.USER_CREATED; }
            public Instant getEventTime() { return evt.getEventTime(); }
        };
    }

}
