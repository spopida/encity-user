package uk.co.encity.user.repositories.mongodb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import reactor.util.Logger;
import reactor.util.Loggers;
import uk.co.encity.user.components.HasUser;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * A 'raw' snapshot of a user at a point in time.  There are no derived fields
 * or business logic in a snapshot.  To obtain a logical entity, all subsequent events
 * up to the desired point in time should be merged with a snapshot.  A snapshot is purely a
 * performance optimisation, and contains database-specific representations
 */
@Getter @Setter @NoArgsConstructor
public class UserSnapshot implements HasUser {
    /**
     * The {@link Logger} for this class
     */
    private static final Logger logger = Loggers.getLogger(UserSnapshot.class);

    @BsonProperty("_id") private ObjectId snapshotId;
    private ObjectId userId;
    private ObjectId tenancyId;
    private String firstName;
    private String lastName;
    private String emailAddress;
    private boolean isAdminUser;

    private int fromVersion;
    private int toVersion;

    private Instant lastUpdate;

    private UserTenantStatus tenantStatus;
    private UserProviderStatus providerStatus;
    private String domain;
    private UUID confirmUUID;
    private Instant expiryTime;
    private Instant userCreationTime;

    /**
     * Copy constructor
     * @param source the {@link UserSnapshot} to copy
     * @return a copy of the <code>source</code> object
     */
    protected UserSnapshot(UserSnapshot source) {
        this.snapshotId = new ObjectId(String.valueOf(source.snapshotId));
        this.userId = new ObjectId(String.valueOf(source.userId));
        this.tenancyId = new ObjectId(String.valueOf(source.tenancyId));
        this.firstName = source.firstName;
        this.lastName = source.lastName;
        this.emailAddress = source.emailAddress;
        this.isAdminUser = source.isAdminUser;
        this.fromVersion = source.fromVersion;
        this.toVersion = source.toVersion;
        this.lastUpdate = source.lastUpdate;
        this.tenantStatus = source.tenantStatus;
        this.providerStatus = source.providerStatus;
        this.domain = source.domain;
        this.confirmUUID = source.confirmUUID;
        this.expiryTime = source.expiryTime;
        this.userCreationTime = source.userCreationTime;
    }


    @BsonProperty("_id") public ObjectId getSnapshotId() { return snapshotId; }
    @BsonProperty("_id") public void setSnapshotId(ObjectId snapshotId) { this.snapshotId = snapshotId; }

    @BsonIgnore
    public int getVersion() { return this.toVersion; }

    @BsonIgnore
    public Instant getCreationTime() { return this.userCreationTime; }

    @BsonIgnore
    @Override
    public String getUserIdentity() { return this.userId.toHexString(); }

    @BsonIgnore
    @Override
    public String getTenancyIdentity() { return this.tenancyId.toHexString(); }

    public User asUser() {
        return new User() {
            public String getUserIdentity() { return userId.toHexString() ; }
            public String getTenancyIdentity() { return tenancyId.toHexString(); }
            public String getFirstName() { return firstName; }
            public String getLastName() { return lastName; }
            public String getEmailAddress() { return emailAddress; }
            public boolean isAdminUser() { return isAdminUser; }
            public int getVersion() { return toVersion; }
            public Instant getLastUpdate() { return lastUpdate; }
            public UserTenantStatus getTenantStatus() { return tenantStatus; }
            public UserProviderStatus getProviderStatus() { return providerStatus; }
            public String getDomain() { return domain; }
            public UUID getConfirmUUID() { return confirmUUID; }
            public Instant getCreationTime() { return userCreationTime; }
            public Instant getExpiryTime() { return expiryTime; }
        };
    }
}
