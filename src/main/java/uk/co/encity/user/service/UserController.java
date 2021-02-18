package uk.co.encity.user.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.util.Logger;
import reactor.util.Loggers;
import uk.co.encity.user.entity.User;
import reactor.core.publisher.Mono;
import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;


@CrossOrigin
@RestController
public class UserController {
    /**
     * The {@link Logger} for this class
     */
    private final Logger logger = Loggers.getLogger(getClass());

    /**
     * The repository of users
     */
    private final UserRepository userRepo;

    /**
     * Construct an instance with access to a repository of users
     * @param repo the instance of {@link UserRepository} that is used to read and write users to and from
     *             persistent storage
     */
    public UserController(@Autowired UserRepository repo) {
        logger.info("Constructing + this.getClass().getName()");
        this.userRepo = repo;
        logger.info("Construction of " + this.getClass().getName() + " is complete");
    }

    /**
     * Attempt to get a JSON representation of a user that is not confirmed and may be confirmed or rejected
     * @param userId the id of the user
     * @param action currently ignored - may be used in future, or removed
     * @param confirmUUID the nonce generated when the user was created to ensure that only the recipient of
     *                    the confirmation request can perform an action
     * @return A {@link uk.co.encity.user.entity.User} represented as a HAL-compliant JSON object
     */
    @GetMapping(value = "/users/{userId}", params = { "action", "uuid"})
    public Mono<ResponseEntity<EntityModel<User>>> getUnconfirmedUser(
        @PathVariable String userId,
        @RequestParam(value = "action") String action,
        @RequestParam(value = "uuid") String confirmUUID)
    {
        logger.debug("Received request to GET user: " + userId + " for confirmation purposes");
        ResponseEntity<EntityModel<User>> response = null;

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
        if (! user.getTenantStatus().equals(UserTenantStatus.UNCONFIRMED)) {
            String message = "Cannot update user " + user.getEmailAddress() + " because it is not UNCONFIRMED.";
            logger.debug(message + "Actual status=" + user.getTenantStatus());
            response = ResponseEntity.status(HttpStatus.CONFLICT).build();
            return Mono.just(response);
        }

        // Has the user been suspended?
        if (! user.getProviderStatus().equals(UserProviderStatus.ACTIVE)) {
            logger.debug(
                    "Cannot update user account as it is not ACTIVE: " + user.getEmailAddress() +
                    ", status=" + user.getProviderStatus()
            );
            response = ResponseEntity.status(HttpStatus.CONFLICT).build();
            return Mono.just(response);
        }

        // Does the UUID match for this confirmation attempt?
        if (! user.getConfirmUUID().equals(confirmUUID)) {
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

        // So far, so good - now add the necessary HAL relations
        EntityModel<User> userEntityModel;

        try {
            userEntityModel = EntityModel.of(user);

            try {
                Method m = UserController.class.getMethod("getUnconfirmedUser", String.class, String.class, String.class);
                Link l = linkTo(m, userId, action, confirmUUID).slash("?action=" + action + "&confirmUUID=" + confirmUUID).withSelfRel();

                userEntityModel.add(l);
            } catch (NoSuchMethodException e) {
                logger.error("Failure generating HAL relations - please investigate.  userId: " + userId);
                response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                return Mono.just(response);
            }
        }
        catch (Exception e) {
            logger.error("Unexpected error returning tenancy - please investigate: " + userId);
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            return Mono.just(response);
        }


        // There's some magic going here that merits further understanding.  The line below appears to
        // convert the object to HAL-compliant JSON (must be functionality in EntityModel class)
        response = ResponseEntity.status(HttpStatus.OK).body(userEntityModel);
        return Mono.just(response);
    }
}
