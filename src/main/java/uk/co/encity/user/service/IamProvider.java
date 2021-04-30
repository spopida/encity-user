package uk.co.encity.user.service;

import uk.co.encity.user.entity.User;

import java.io.IOException;

public interface IamProvider {
    public void createUser(User user) throws IOException;
}
