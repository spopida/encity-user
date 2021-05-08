package uk.co.encity.user.repositories.mongodb;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@ConfigurationProperties(prefix = "uk.co.encity.user")
@ConstructorBinding
public class RepositoryConfig {

    private final String adminRoleId;
    private final String portfolioUserRoleId;

    public RepositoryConfig(String adminRoleId, String portfolioUserRoleId) {
        this.adminRoleId = adminRoleId;
        this.portfolioUserRoleId = portfolioUserRoleId;
    }
}
