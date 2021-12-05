package uk.co.encity.user.entity;

import lombok.Getter;

@Getter
public class BasicUser {

    private String id;
    private String emailAddress;
    private boolean adminUser;
    private String firstName;
    private String lastName;

    public BasicUser( User u) {
        this.id = u.getUserId();
        this.emailAddress = u.getEmailAddress();
        this.adminUser = u.isAdminUser();
        this.firstName = u.getFirstName();
        this.lastName = u.getLastName();
    }
}
