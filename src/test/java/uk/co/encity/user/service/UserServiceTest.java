package uk.co.encity.user.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.co.encity.user.commands.ConfirmUserCommand;
import uk.co.encity.user.commands.PreConditionException;
import uk.co.encity.user.commands.RejectUserCommand;
import uk.co.encity.user.entity.User;
import uk.co.encity.user.entity.UserProviderStatus;
import uk.co.encity.user.entity.UserTenantStatus;
import uk.co.encity.user.service.jackson.JacksonUserService;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { MockitoExtension.class })
public class UserServiceTest {
    // Class to be tested
    private UserService userService;

    private Map extras;

    // Dependencies
    @Mock
    private UserRepository userRepo;

    @Mock
    private RabbitTemplate rabbitTemplate;

    // This sucks a bit - when mocking a User we have to provide a mixin definition because
    // otherwise Jackson serialization struggles with the mock sub-type
    // See this link for further info: https://stackoverflow.com/questions/22851462/infinite-recursion-when-serializing-objects-with-jackson-and-mockito
    @Mock
    private User aUser;

    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    interface UserMixin {
        @JsonProperty public String getUserId();
        @JsonProperty public String getFirstName();
        @JsonProperty public String getLastName();
        @JsonProperty public String getEmailAddress();
        @JsonProperty public boolean isAdminUser();
        @JsonProperty public int getVersion();
        @JsonProperty public Instant getLastUpdate();
        @JsonProperty public UserTenantStatus getTenantStatus();
        @JsonProperty public UserProviderStatus getProviderStatus();
        @JsonProperty public String getDomain();
        @JsonProperty public UUID getConfirmUUID();
        @JsonProperty public Instant getCreationTime();
        @JsonProperty public Instant getExpiryTime();
    }

    @BeforeEach
    public void setup() {
        // Force jackson serialization to use real getters for User objects
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(User.class, UserMixin.class);
        this.userService = new JacksonUserService(userRepo, rabbitTemplate, mapper);

        // Create some command-specific data
        this.extras = new HashMap();
        this.extras.put(ConfirmUserCommand.Extras.INITIAL_PASSWORD, "BoiledFrogs");
    }

    @Nested
    public class TheApplyCommandMethod {
        @Test
        public void allows_An_Unconfirmed_User_To_Be_Confirmed() throws
                IOException,
                PreConditionException
        {
            when(aUser.getTenantStatus()).thenReturn(UserTenantStatus.UNCONFIRMED);
            when(aUser.getProviderStatus()).thenReturn(UserProviderStatus.ACTIVE);
            when(userRepo.getUser(anyString())).thenReturn(aUser);

            userService.applyCommand(new ConfirmUserCommand("1234", userRepo, extras ));
        }

        @Test
        public void allows_An_Unconfirmed_User_To_Be_Rejected() throws
                IOException,
                PreConditionException
        {
            when(aUser.getTenantStatus()).thenReturn(UserTenantStatus.UNCONFIRMED);
            when(aUser.getProviderStatus()).thenReturn(UserProviderStatus.ACTIVE);
            when(userRepo.getUser(anyString())).thenReturn(aUser);
            userService.applyCommand(new RejectUserCommand("1234", userRepo ));
        }

        @Test
        public void prevents_A_Confirmed_User_From_Being_Confirmed() throws
                IOException
        {
            when(aUser.getTenantStatus()).thenReturn(UserTenantStatus.CONFIRMED);
            when(aUser.getProviderStatus()).thenReturn(UserProviderStatus.ACTIVE);
            when(userRepo.getUser(anyString())).thenReturn(aUser);
            assertThrows(PreConditionException.class,() -> {
                userService.applyCommand(new ConfirmUserCommand("1234", userRepo, extras ));
            });
        }
        @Test
        public void prevents_A_Confirmed_User_From_Being_Rejected() throws
                IOException
        {
            when(aUser.getTenantStatus()).thenReturn(UserTenantStatus.CONFIRMED);
            when(aUser.getProviderStatus()).thenReturn(UserProviderStatus.ACTIVE);
            when(userRepo.getUser(anyString())).thenReturn(aUser);
            assertThrows(PreConditionException.class,() -> {
                userService.applyCommand(new RejectUserCommand("1234", userRepo ));
            });
        }
    }
}
