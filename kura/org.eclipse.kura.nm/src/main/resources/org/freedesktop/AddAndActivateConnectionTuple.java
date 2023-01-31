package org.freedesktop;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class AddAndActivateConnectionTuple extends Tuple {

    @Position(0)
    private DBusPath path;
    @Position(1)
    private DBusPath activeConnection;

    public AddAndActivateConnectionTuple(DBusPath path, DBusPath activeConnection) {
        this.path = path;
        this.activeConnection = activeConnection;
    }

    public void setPath(DBusPath arg) {
        path = arg;
    }

    public DBusPath getPath() {
        return path;
    }

    public void setActiveConnection(DBusPath arg) {
        activeConnection = arg;
    }

    public DBusPath getActiveConnection() {
        return activeConnection;
    }

}