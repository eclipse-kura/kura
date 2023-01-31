package org.freedesktop.networkmanager;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class NeedSecretsTuple extends Tuple {
    @Position(0)
    private String username;
    @Position(1)
    private String password;

    public NeedSecretsTuple(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setUsername(String arg) {
        username = arg;
    }

    public String getUsername() {
        return username;
    }
    public void setPassword(String arg) {
        password = arg;
    }

    public String getPassword() {
        return password;
    }


}