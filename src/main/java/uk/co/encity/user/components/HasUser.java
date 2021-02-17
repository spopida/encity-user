package uk.co.encity.user.components;

import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;

import java.time.Instant;

public interface HasUser {
    public String getUserId();

    public String getTenancyId();

    public String getFirstName();

    public String getLastName();

    public String getEmailAddress();

    public boolean isAdminUser();

    public int getVersion();

    public Instant getLastUpdate();

    public UserTenantStatus getTenantStatus();

    public UserProviderStatus getProviderStatus();

}
