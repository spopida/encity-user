package uk.co.encity.user.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import uk.co.encity.user.repositories.mongodb.RepositoryConfig;

@EnableConfigurationProperties(RepositoryConfig.class)
@SpringBootApplication(scanBasePackages = { "uk.co.encity.user" }, exclude={MongoAutoConfiguration.class})
public class UserApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserApplication.class, args);
	}
}
