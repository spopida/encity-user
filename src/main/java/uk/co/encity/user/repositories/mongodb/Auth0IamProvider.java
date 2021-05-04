package uk.co.encity.user.repositories.mongodb;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Component;
import reactor.util.Logger;
import reactor.util.Loggers;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.service.IamProvider;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
//import java.net.http.HttpResponse;

@Component
@EnableScheduling
@EnableAsync
public class Auth0IamProvider implements IamProvider {

    /**
     * The number of seconds to cut off the access token expiry delay before regenerating a new token.
     *
     * This is done to avoid the risk of expiry if network latency delays the regeneration request.
     */
    private final static long ACCESS_TOKEN_EXPIRY_HAIRCUT_SECS = 60;

    /**
     * A POJO type for access tokens
     *
     * Note that getters and setters in this class are synchronized to make them thread-safe.  This
     * allows client code (in this case the outer class) to schedule separate threads to update the token
     * (separate, that is, from threads that read the token)
     */
    @Builder
    @Getter(onMethod_={@Synchronized})
    @Setter(onMethod_={@Synchronized})
    @AllArgsConstructor @NoArgsConstructor
    public static class Auth0MgmtApiAccessToken {
        private String accessToken;
        private long expiresIn;
        private String tokenType;
        // TODO: add scopes here
        private LocalDateTime nextTokenDue;
    }

    /**
     * A type for deserializing access tokens from their JSON representation into a POJO
     */
    class Auth0MgmtApiAccessTokenDeserializer extends StdDeserializer<Auth0MgmtApiAccessToken> {
        public Auth0MgmtApiAccessTokenDeserializer() { this( null); }
        public Auth0MgmtApiAccessTokenDeserializer(Class<?> valueClass) { super(valueClass); }

        @Override
        public Auth0MgmtApiAccessToken deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

            JsonNode node = jp.getCodec().readTree(jp);

            long expiryInSecs = node.get("expires_in").asLong();

            return new Auth0IamProvider.Auth0MgmtApiAccessToken().builder()
                    .accessToken(node.get("access_token").asText())
                    .expiresIn(expiryInSecs)
                    .tokenType(node.get("token_type").asText())
                    .nextTokenDue(LocalDateTime.now().plusSeconds(expiryInSecs))
                .build();
        }
    }

    /**
     * The {@link Logger} for this class
     */
    private final Logger logger = Loggers.getLogger(getClass());

    private String iamTokenEndpoint;
    private String iamAudience;
    private String iamClientId;
    private String iamClientSecret;

    private Auth0MgmtApiAccessToken mgmtAPIAccessToken;

    /**
     * Construct an instance of the Auth0 provider.
     * @param iamTokenEndpoint the endpoint of the management API for getting an access token
     * @param iamAudience the identity of the audience (management API application)
     * @param iamClientId the client id of this application for machine-to-machine interaction with the management API
     * @param iamClientSecret the client secret
     */
    public Auth0IamProvider(
            @Value("${uk.co.encity.user.iamprovider.token-endpoint}") String iamTokenEndpoint,
            @Value("${uk.co.encity.user.iamprovider.audience}") String iamAudience,
            @Value("${uk.co.encity.user.m2m.client-id}") String iamClientId,
            @Value("${uk.co.encity.user.m2m.client-secret}") String iamClientSecret
    )
    {
        this.iamTokenEndpoint = iamTokenEndpoint;
        this.iamAudience = iamAudience;
        this.iamClientId = iamClientId;
        this.iamClientSecret = iamClientSecret;

        // Get a new access token for the management API on start-up
        this.getNewAccessToken();
    }

    /**
     * An {@link Runnable} used for getting an access token
     */
    private class AccessTokenFetcher implements Runnable {
        @Override
        public void run() {
            Auth0IamProvider.this.logger.info("Generating another access token");
            Auth0IamProvider.this.getNewAccessToken();
        }
    }
    // TODO: think about synchronisation of method calls in relation to this thread

    /**
     * Get a new access token for use when calling the management API.
     *
     * On successful acquisition of a token, check the expiry time, and schedule the future generation of
     * another token before that time is reached.  This should enable method calls to run smoothly for the
     * lifetime of the application.  Whenever a method call occurs, there should always be an unexpired token.
     */
    @Async
    void getNewAccessToken() {
        logger.info("Generating new access token for Auth0 Management API calls");

        String body =
                "grant_type=client_credentials&" +
                String.format("client_id=%s&", this.iamClientId) +
                String.format("client_secret=%s&", this.iamClientSecret) +
                String.format("audience=%s", this.iamAudience);

        HttpResponse<String> response = Unirest.post(this.iamTokenEndpoint)
                .header("content-type", "application/x-www-form-urlencoded")
                .body(body)
                .asString();

        // Note, for this to work the M2M Application (aka API) needs to be authorised to use the Management API
        String tokenResponse = response.getBody();

        // Check the status code
        int responseCode = response.getStatus();
        if (responseCode == HttpStatus.OK.value()) {
            logger.debug("Received access token response");

            // Extract the access token from the JSON - stick in this object to remember it
            ObjectMapper mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(Auth0MgmtApiAccessToken.class, new Auth0MgmtApiAccessTokenDeserializer());
            mapper.registerModule(module);

            try {
                this.mgmtAPIAccessToken = mapper.readValue(tokenResponse, Auth0MgmtApiAccessToken.class);
                logger.info("Access token extraction successful");

                // Schedule creation of a new access code
                //Date now = new Date();
                //Date then = new Date(now.getTime() + ((this.mgmtAPIAccessToken.getExpiresIn() - 60) * 1000));

                LocalDateTime rightNow = LocalDateTime.now();
                LocalDateTime rightThen =
                        rightNow.plusSeconds(this.mgmtAPIAccessToken.getExpiresIn() - ACCESS_TOKEN_EXPIRY_HAIRCUT_SECS);

                ScheduledExecutorService localExecutor = Executors.newSingleThreadScheduledExecutor();
                TaskScheduler scheduler = new ConcurrentTaskScheduler(localExecutor);

                Date then = Date.from(((LocalDateTime)rightThen).atZone(ZoneId.systemDefault()).toInstant());
                scheduler.schedule(new AccessTokenFetcher(), then);
                logger.info("Next access token acquisition is scheduled for " + then.toString());
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse access token", e);
            }
        }
        else {
            logger.error(
                String.format("Access token retrieval failed with response %s: %s",
                response.getStatus(),
                response.getStatusText())
            );
        }

        return;
    }

    @Override
    public void createUser(User user) throws IOException {
        // TODO: Allow the type of user to determine what scopes get created, and create the scopes

        final String CONNECTION_NAME = "Username-Password-Authentication";

        /**
         * Local POJO for the Auth0 representation of a user
         */
        @Getter @Setter @AllArgsConstructor
        class Auth0User {
            private String connection;
            private String email;
            private boolean emailVerified;
            private String name;
            private boolean verifyEmail = false;
            private String username;
            private String password;

        }

        /**
         * And a serializer for Auth0User
         */
        class Auth0UserSerializer extends StdSerializer<Auth0User> {

            public Auth0UserSerializer() { this(null); }
            public Auth0UserSerializer(Class<Auth0User> u) { super(u); }

            @Override
            public void serialize(Auth0User auth0User, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                    throws IOException {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("connection", auth0User.getConnection());
                jsonGenerator.writeStringField("email", auth0User.getEmail());
                jsonGenerator.writeBooleanField("email_verified", auth0User.isEmailVerified());
                jsonGenerator.writeStringField("name", auth0User.getName());
                jsonGenerator.writeBooleanField("verify_email", auth0User.isVerifyEmail());
                jsonGenerator.writeStringField("user_id", auth0User.getUsername());
                jsonGenerator.writeStringField("password", auth0User.getPassword());
                jsonGenerator.writeEndObject();
            }
        }
        // Call the Auth0 Management API to create the user

        // 1. Check that we seem to have an access token that hasn't expired
        LocalDateTime now = LocalDateTime.now();
        if (Duration.between(now, this.mgmtAPIAccessToken.nextTokenDue).toSeconds() < ACCESS_TOKEN_EXPIRY_HAIRCUT_SECS) {
            logger.warn("Unexpected imminent expiry of access token - attempting re-generation");
            this.getNewAccessToken();
        }

        // 2. Serialize a POJO representing the (external) user
        Auth0User auth0User = new Auth0User(
                CONNECTION_NAME,
                user.getEmailAddress(),
                true,
                String.format("%s %s", user.getFirstName(), user.getLastName()),
                false,
                user.getUserId(),
                "!HubbleBubble3004!");

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Auth0User.class, new Auth0UserSerializer());
        mapper.registerModule(module);
        String jsonAuth0User = mapper.writeValueAsString(auth0User);

        // 3. POST the Auth0 user (JSON representation) to the Auth0 endpoint
        HttpResponse<String> response = Unirest.post(this.iamAudience + "users")
                .header("content-type", "application/json")
                .header("authorization", "Bearer " + this.mgmtAPIAccessToken.getAccessToken())
                .body(jsonAuth0User)
                .asString();

        // 4. Check whether it worked
        if (response.getStatus() != HttpStatus.CREATED.value()) {
            final String msg = String.format("Failed to create user %s due to: %s", user.getEmailAddress(), response.getStatusText());
            logger.error(msg);
            throw new IOException(msg);
        }

        return;
    }
}
