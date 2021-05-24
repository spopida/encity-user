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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.util.Logger;
import reactor.util.Loggers;
import uk.co.encity.user.commands.PatchUserCommand;
import uk.co.encity.user.commands.UserCommand;
import uk.co.encity.user.components.EmailRecipient;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;
import uk.co.encity.user.events.generated.UserEvent;
import uk.co.encity.user.events.generated.UserEventType;
import uk.co.encity.user.service.IamProvider;
import uk.co.encity.user.service.UserRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/* MongoDB Reactive - coming soon
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.Success;
*/

import static com.mongodb.client.model.Filters.*;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Component
public class MongoDBUserRepository implements UserRepository {

    /**
     * The external Identity and Access Management Provider
     */
    private final IamProvider iamProvider;

    /**
     * The {@link Logger} for this class
     */
    private final Logger logger = Loggers.getLogger(getClass());

    private final MongoClient mongoClient;
    private final MongoDatabase db;
    private final CodecRegistry codecRegistry;

    // TODO: Does this belong in the repo - maybe should move it outside?
    @Value("${user.expiryHours}")
    int expiryHours;

    @Override
    public String getIdentity() { return new ObjectId().toHexString(); }

    public MongoDBUserRepository(
            @Value("${mongodb.uri}") String mongodbURI,
            @Value("${user.db}") String dbName,
            @Autowired IamProvider iamProvider,
            @Autowired RepositoryConfig repoConfig)
    {
        this.iamProvider = iamProvider;

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
        UserSnapshot snap = snapshots.find(eq("userIdentity", targetId)).sort(new BasicDBObject("lastUpdate", -1)).first();

        return snap;
    }

    private User inflate(final UserSnapshot snap) throws IOException {

        UserSnapshot snapCopy = null;

        if (snap != null) {
            snapCopy =  new UserSnapshot(snap);

            // Get all events since for the user, in chronological order
            List<MongoDBUserEvent> events = getEventRange(snap.getUserId(), snap.getToVersion());
            for (MongoDBUserEvent e : events) {
                snapCopy = e.updateUser(snapCopy);
            }
        }
        return snapCopy.asUser();
    }

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

    @Override
    public User getUser(String userId) throws IOException {
        UserSnapshot latestSnap = this.getLatestSnapshot(userId);
        return this.inflate(latestSnap);
    }

    @Override
    public User confirmUser(User user, String initialPassword) throws IOException {
        // Attempt to create the external representation of the User using the IAMProvider
        iamProvider.createUser(user, initialPassword);
        return user;
    }

    @Override
    public User addUser(String tenancyId, String domain, EmailRecipient user, boolean isAdmin) throws IOException {
        // Create a snapshot
        Instant now = Instant.now();
        UserSnapshot snap = new UserSnapshot();
        snap.setUserIdentity(new ObjectId());
        snap.setTenancyIdentity(new ObjectId(tenancyId));
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

        // Create a User from the snapshot
        User u = new User() {
            public String getUserId() { return snap.getUserId(); }
            public String getTenancyId() { return snap.getTenancyId(); }
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

        // Store the snapshot
        MongoCollection<UserSnapshot> userSnapshots = db.getCollection("user_snapshots", UserSnapshot.class);
        userSnapshots.insertOne(snap);

        // Return the user
        return u;
    }

    @Override
    public UserEvent addUserEvent(String commandId, UserEventType type, User user) {
        final MongoDBUserEvent evt;
        Instant now = Instant.now();
        switch (type) {
            case USER_CREATED:
                evt = MongoDBUserCreatedEvent.builder()
                        .userId(new ObjectId(user.getUserId()))
                        .eventTime(now)
                        .userVersionNumber(1)
                        .userEventType(type)
                        .expiryTime(user.getExpiryTime())
                        .commandId(new ObjectId(commandId))
                        .build();
                break;
            case USER_CONFIRMED:
                evt = MongoDBUserConfirmedEvent.builder()
                        .userId(new ObjectId(user.getUserId()))
                        .eventTime(now)
                        .userVersionNumber(user.getVersion() + 1)
                        .userEventType(type)
                        .commandId(new ObjectId(commandId))
                        .build();
                break;
            case USER_REJECTED:
                evt = MongoDBUserRejectedEvent.builder()
                        .userId(new ObjectId(user.getUserId()))
                        .eventTime(now)
                        .userVersionNumber(user.getVersion() + 1)
                        .userEventType(type)
                        .commandId(new ObjectId(commandId))
                        .build();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }

        MongoCollection<MongoDBUserEvent> events = db.getCollection("user_events", MongoDBUserEvent.class);
        events.insertOne(evt);

        return evt.asUserEvent(commandId, user, this);
    }

    @Override
    public PatchUserCommand addPatchUserCommand(UserCommand.UserTenantCommandType type, PatchUserCommand cmd) {
        MongoCollection<MongoDBUserCommand> commands = db.getCollection("user_commands", MongoDBUserCommand.class);

        MongoDBPatchUserCommand dbCmd = MongoDBPatchUserCommand.getMongoDBPatchUserCommand(cmd);
        commands.insertOne(dbCmd);

        return cmd;
    }
}
