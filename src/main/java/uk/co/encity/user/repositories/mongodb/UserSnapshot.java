package uk.co.encity.user.repositories.mongodb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import reactor.util.Logger;
import reactor.util.Loggers;
import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;

import java.time.Instant;

/**
 * A 'raw' snapshot of a user at a point in time.  There are no derived fields
 * or business logic in a snapshot.  To obtain a logical entity, all subsequent events
 * up to the desired point in time should be merged with a snapshot.  A snapshot is purely a
 * performance optimisation, and contains database-specific representations
 */
@Getter @Setter @NoArgsConstructor
public class UserSnapshot {
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

    @BsonProperty("_id") public ObjectId getSnapshotId() { return snapshotId; }
    @BsonProperty("_id") public void setSnapshotId(ObjectId snapshotId) { this.snapshotId = snapshotId; }
}
