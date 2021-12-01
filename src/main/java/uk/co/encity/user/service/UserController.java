package uk.co.encity.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import uk.co.encity.user.commands.PatchUserCommand;
import uk.co.encity.user.commands.PatchUserCommandDeserializer;
import uk.co.encity.user.commands.PreConditionException;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;

import java.io.IOException;
import java.lang.reflect.Array;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import java.util.ArrayList;

@CrossOrigin
@RestController
public class UserController {
    /**
     * The name of the AMQP exchange used for message publication
     */
    private static final String topicExchangeName = "encity-exchange";

    /**
     * The {@link Logger} for this class
     */
    private final Logger logger = Loggers.getLogger(getClass());

    /**
     * The repository of users
     */
    private final UserRepository userRepo;

    /**
     * The RabbitMQ helper class
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * The service - orchestrates actions
     */
    private final UserService userService;

    /**
     * Construct an instance with access to a repository of users
     *
     * @param repo the instance of {@link UserRepository} that is used to read and write users to and from
     *             persistent storage
     */
    public UserController(
            @Autowired UserRepository repo,
            @Autowired RabbitTemplate rabbitTmpl,
            @Autowired UserService service) {
        logger.debug(String.format("Constructing %s", this.getClass().getName()));
        this.userRepo = repo;
        this.rabbitTemplate = rabbitTmpl;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        this.userService = service;

        logger.debug("Construction of " + this.getClass().getName() + " is complete");
    }

    @CrossOrigin
    @PreAuthorize("permitAll()")
    @GetMapping(value = "/users", params = {"tenancyId"})
    public Mono<ResponseEntity<ArrayList<User>>> getUsersForTenancy(
            @RequestParam(value = "tenancyId") String tenancyId) {

        logger.debug("Received request to GET users for tenancy: " + tenancyId);
        ResponseEntity<ArrayList<User>> response = null;

        ArrayList<User> userArrayList = new ArrayList<User>();

        userArrayList.add(getNewUser(tenancyId, "gmurtagh@ptrconsulting.com", true, "Gerald", "Murtagh"));
        userArrayList.add(getNewUser(tenancyId, "jbrook@teazel.com", true, "brook", "brook"));

        response = ResponseEntity.status(HttpStatus.OK).body(userArrayList);
        return Mono.just(response);
    }

    private User getNewUser( String tenancyId, String emailAddress, Boolean isAdminUser, String firstName, String lastName ) {
        return new User() {
            public String getUserId() { return "UserId"; }
            public String getTenancyId() { return tenancyId; }
            public String getFirstName() { return firstName; }
            public String getLastName() { return lastName; }
            public String getEmailAddress() { return emailAddress; }
            public boolean isAdminUser() { return isAdminUser; }
            public int getVersion() { return 1; }
            public Instant getLastUpdate() { return Instant.now(); }
            public UserTenantStatus getTenantStatus() { return UserTenantStatus.CONFIRMED; }
            public UserProviderStatus getProviderStatus() { return UserProviderStatus.ACTIVE; }
            public String getDomain() { return "domain"; }
            public UUID getConfirmUUID() { return UUID.randomUUID(); }
            public Instant getCreationTime() { return Instant.now(); }
            public Instant getExpiryTime() { return Instant.now(); }

        };
    }

    /**
     * Attempt to get a JSON representation of a user that is not confirmed and may be confirmed or rejected
     *
     * @param userId      the id of the user
     * @param action      currently ignored - may be used in future, or removed
     * @param confirmUUID the nonce generated when the user was created to ensure that only the recipient of
     *                    the confirmation request can perform an action
     * @return A {@link uk.co.encity.user.entity.User} represented as a JSON object
     */
    @CrossOrigin
    @PreAuthorize("permitAll()")
    @GetMapping(value = "/users/{userId}", params = {"action", "uuid"})
    public Mono<ResponseEntity<User>> getUnconfirmedUser(
            @PathVariable String userId,
            @RequestParam(value = "action") String action,
            @RequestParam(value = "uuid") String confirmUUID) {

        logger.debug("Received request to GET user: " + userId + " for confirmation purposes");
        ResponseEntity<User> response = null;

        // Retrieve the (inflated) user entity
        User user = null;
        try {
            user = this.userRepo.getUser(userId);
            if (user == null) {
                response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                return Mono.just(response);
            }
        } catch (IOException e) {
            String msg = "Unexpected failure reading user with id: " + userId;
            logger.error(msg);
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            return Mono.just(response);
        }

        // TODO: implement proper Spring exception/response handling in the sections below

        // Is confirmation still pending?
        if (!user.getTenantStatus().equals(UserTenantStatus.UNCONFIRMED)) {
            String message = "Cannot update user " + user.getEmailAddress() + " because it is not UNCONFIRMED.";
            logger.debug(message + "Actual status=" + user.getTenantStatus());
            response = ResponseEntity.status(HttpStatus.CONFLICT).build();
            return Mono.just(response);
        }

        // Has the user been suspended?
        if (!user.getProviderStatus().equals(UserProviderStatus.ACTIVE)) {
            logger.debug(
                    "Cannot update user account as it is not ACTIVE: " + user.getEmailAddress() +
                            ", status=" + user.getProviderStatus()
            );
            response = ResponseEntity.status(HttpStatus.CONFLICT).build();
            return Mono.just(response);
        }

        // Does the UUID match for this confirmation attempt?
        if (!user.getConfirmUUID().toString().equals(confirmUUID)) {
            logger.warn(
                    "Attempt to confirm a user with mis-matched UUIDs.  Incoming: " + confirmUUID +
                            ", target=" + user.getConfirmUUID() + ".\n" +
                            "Repeated attempts with different UUIDs might indicate suspicious activity.");
            response = ResponseEntity.status(HttpStatus.CONFLICT).build();
            return Mono.just(response);
        }

        // Has the confirmation window expired?
        int compareResult = Instant.now().compareTo(user.getExpiryTime());
        if (compareResult > 0) {
            logger.debug("User confirmation window expired at: " + user.getExpiryTime().toString());
            response = ResponseEntity.status(HttpStatus.CONFLICT).build();
            return Mono.just(response);
        }

        response = ResponseEntity.status(HttpStatus.OK).body(user);
        return Mono.just(response);
    }

    /**
     * Attempt to patch a user
     */
    @CrossOrigin
    @PreAuthorize("permitAll()")
    @PatchMapping(value = "/users/{id}")
    public Mono<ResponseEntity<User>> patchUser(
            @PathVariable String id,
            @RequestBody String body,
            UriComponentsBuilder uriBuilder) {

        logger.debug("Attempting to patch a user from request body:\n" + body);

        //-------------------------------------------------------
        // 1. Analyse and validate and store the incoming command
        //-------------------------------------------------------
        // TODO: refactor this section into a separate method so that it can be unit tested
        ResponseEntity<User> response = null;

        // Figure out the type of update
        //  - validate against a generic patch schema that checks the command is supported
        //  - validate the patch data against a specific schema (when more transitions are implemented)

        // The schema contains an enumeration of possible patch sub-types (confirm, reject, etc)
        final String patchUserCommandSchema = "command-schemas/patch-user-command.json";

        try {
            Schema schema = SchemaLoader.load(
                    new JSONObject(
                            new JSONTokener(requireNonNull(getClass().getClassLoader().getResourceAsStream(patchUserCommandSchema)))
                    )
            );
            JSONObject patch = new JSONObject(new JSONTokener(body));
            schema.validate(patch);
            logger.debug("Incoming patch request contains valid request type");

            // As we add more transitions, additional validation will be needed here
            // We should create specific schemas per transition and also validate against them
        } catch (ValidationException e) {
            logger.warn("Incoming request body does NOT validate against patch schema; potential API mis-use!");
            response = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            return Mono.just(response);
        }

        // De-serialise the command into an object and store it
        PatchUserCommand cmd = null;

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(PatchUserCommand.class, new PatchUserCommandDeserializer(id, userRepo));
        mapper.registerModule(module);

        try {
            cmd = mapper.readValue(body, PatchUserCommand.class);
            logger.debug("Patch tenancy command de-serialised successfully");
        } catch (IOException e) {
            logger.error("Error de-serialising patch tenancy command: " + e.getMessage());
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            return Mono.just(response);
        }

        // Store the command - even if it doesn't 'execute'
        userRepo.addPatchUserCommand(cmd.getCmdType(), cmd);

        //-------------------------------------------------------
        // 2. Execute the command
        //-------------------------------------------------------
        User u = null;
        try {
            u = userService.applyCommand(cmd);
        } catch (UnsupportedOperationException | IOException e) {
            logger.error(e.getMessage());
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            return Mono.just(response);
        } catch (IllegalArgumentException | PreConditionException e) {
            logger.info(e.getMessage());
            response = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            return Mono.just(response);
        }

        // Build a response (include the correct location)
        UriComponents uriComponents = uriBuilder.path("/users/" + u.getUserId()).build();
        HttpHeaders headers =  new HttpHeaders();
        headers.setLocation(uriComponents.toUri());

        response = ResponseEntity.status(HttpStatus.OK).headers(headers).body(u);

        return Mono.just(response);
    }

    /**
     * Attempt to get user info.
     * @param id the identity of the user
     * @return  A Mono that wraps a ResponseEntity containing the response.  Possible
     *          response status codes are INTERNAL_SERVER_ERROR, OK, and NOT_FOUND.
     */
    @CrossOrigin
    @PreAuthorize("hasAuthority('SCOPE_read:user_profile')")
    @GetMapping(value = "/users/{id}", params = {})
    public Mono<ResponseEntity<User>> getUser(@PathVariable String id) {
        logger.debug("Attempting to GET user: " + id);
        ResponseEntity<User> response = null;

        User theUser = null;
        try {
            theUser = userRepo.getUser(id);
        } catch (IOException e) {
            logger.debug("Error retrieving user: " + theUser.getUserId());
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            return Mono.just(response);
        }

        if (theUser == null) {
            response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            return Mono.just(response);
        }

        try {
            response = ResponseEntity.status(HttpStatus.OK).body(theUser);
            //response = ResponseEntity.status(HttpStatus.OK).body(this.getHateoasEntityModel(theUser));
            return Mono.just(response);
        } catch (Exception e) {
            logger.error(String.format("Unexpected error: %s", e.getMessage()));
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            return Mono.just(response);
        }
    }
}
