package uk.co.encity.user.components;

import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;

import java.time.Instant;
import java.util.UUID;

public interface HasUser {
    public String getUserIdentity();

    public String getTenancyIdentity();

    public String getFirstName();

    public String getLastName();

    public String getEmailAddress();

    public boolean isAdminUser();

    public int getVersion();

    public Instant getLastUpdate();

    public UserTenantStatus getTenantStatus();

    public UserProviderStatus getProviderStatus();

    public String getDomain();

    public UUID getConfirmUUID();

    public Instant getCreationTime();

    public Instant getExpiryTime();

}
