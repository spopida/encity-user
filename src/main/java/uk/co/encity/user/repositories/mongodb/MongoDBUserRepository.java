package uk.co.encity.user.repositories.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
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

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Filters.*;
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

    @Value("${user.expiryHours}")
    int expiryHours;

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

    private UserSnapshot getLatestSnapshot(String id) {
        ObjectId targetId = new ObjectId(id);
        MongoCollection<UserSnapshot> snapshots = db.getCollection("user_snapshots").withDocumentClass(UserSnapshot.class);
        UserSnapshot snap = snapshots.find(eq("userId", targetId)).sort(new BasicDBObject("lastUpdate", -1)).first();

        return snap;
    }

    private UserSnapshot inflate(final UserSnapshot snap) throws IOException {

        UserSnapshot inflated = new UserSnapshot(snap);

        if (inflated != null) {

            // Get all events since for the user, in chronological order
            List<MongoDBUserEvent> events = getEventRange(inflated.getUserIdentity(), snap.getToVersion());
            for (MongoDBUserEvent e : events) {
                inflated = e.updateUserSnapshot(inflated);
            }
        }

        return inflated;
    }

    // TODO: Convert to internal classes (not interfaces)!
    private List<MongoDBUserEvent> getEventRange(String userId, int fromVersion) {
        List<MongoDBUserEvent> evtList = new ArrayList<>();

        MongoCollection<MongoDBUserEvent> events = db.getCollection("user_events", MongoDBUserEvent.class);

        // Define a query that finds the right versions and sorts them
        ObjectId uId = new ObjectId(userId);
        FindIterable<MongoDBUserEvent> evts = events.find(and(eq("userId", uId), gt("userVersionNumber", fromVersion)));

        for (MongoDBUserEvent e : evts) {
            evtList.add(e);
        }
        return evtList;
    }
/*
    private List<UserEvent> getEventRange(String userId, int fromVersion) {
        List<UserEvent> evtList = new ArrayList<>();

        MongoCollection<UserEvent> events = db.getCollection("user_events", UserEvent.class);

        // Define a query that finds the right versions and sorts them
        ObjectId uId = new ObjectId(userId);
        FindIterable<UserEvent> evts = events.find(and(eq("userId", uId), gt("userVersionNumber", fromVersion)));

        for (UserEvent e : evts) {
            evtList.add(e);
        }
        return evtList;
    }
*/

    @Override
    public User getUser(String userId) throws IOException {
        UserSnapshot latestSnap = this.getLatestSnapshot(userId);
        return this.inflate(latestSnap).asUser();
    }

    @Override
    public User addUser(String tenancyId, String domain, EmailRecipient user, boolean isAdmin) {
        // Create a snapshot
        Instant now = Instant.now();
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
        snap.setLastUpdate(now);
        snap.setDomain(domain);
        snap.setConfirmUUID(UUID.randomUUID());
        snap.setExpiryTime(now.plus(this.expiryHours, ChronoUnit.HOURS));
        snap.setUserCreationTime(now);

        // Store the snapshot
        MongoCollection<UserSnapshot> userSnapshots = db.getCollection("user_snapshots", UserSnapshot.class);
        userSnapshots.insertOne(snap);

        // Return the user
        return new User() {
            public String getUserIdentity() { return snap.getUserIdentity(); }
            public String getTenancyIdentity() { return snap.getTenancyIdentity(); }
            public String getFirstName() { return snap.getFirstName(); }
            public String getLastName() { return snap.getLastName(); }
            public String getEmailAddress() { return snap.getEmailAddress(); }
            public boolean isAdminUser() { return snap.isAdminUser(); }
            public int getVersion() { return snap.getToVersion(); }
            public Instant getLastUpdate() { return snap.getLastUpdate(); }
            public UserTenantStatus getTenantStatus() { return snap.getTenantStatus(); }
            public UserProviderStatus getProviderStatus() { return snap.getProviderStatus(); }
            public String getDomain() { return snap.getDomain(); }
            public UUID getConfirmUUID() { return snap.getConfirmUUID(); }
            public Instant getCreationTime() { return now; }
            public Instant getExpiryTime() { return snap.getExpiryTime(); }
        };

    }

    @Override
    public UserEvent addUserEvent(UserEventType type, User user) {
        final MongoDBUserEvent evt;
        Instant now = Instant.now();
        switch (type) {
            case USER_CREATED:
                evt = MongoDBUserCreatedEvent.builder()
                        .userId(new ObjectId(user.getUserIdentity()))
                        .eventTime(now)
                        .userVersionNumber(user.getVersion())
                        .userEventType(type)
                        .expiryTime(user.getExpiryTime())
                        .build();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }

        MongoCollection<MongoDBUserEvent> events = db.getCollection("user_events", MongoDBUserEvent.class);
        events.insertOne(evt);

        return new UserCreatedEvent() {
            public String getUserIdentity() { return user.getUserIdentity(); }
            public String getTenancyIdentity() { return user.getTenancyIdentity();}
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
            public String getDomain() { return user.getDomain(); }
            public UUID getConfirmUUID() { return user.getConfirmUUID(); };
            public Instant getCreationTime() { return user.getCreationTime(); }
            public Instant getExpiryTime() { return user.getExpiryTime(); }
        };
    }

}
