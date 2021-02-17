package uk.co.encity.user.entity;

import uk.co.encity.user.components.HasUser;

import java.time.Instant;

/**
 * simple view of a user that only has attributes that should
 * be externally visible
 */
public interface User extends HasUser {
}
