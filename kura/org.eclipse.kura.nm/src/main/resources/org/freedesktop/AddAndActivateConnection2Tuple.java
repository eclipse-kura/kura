package org.freedesktop;

import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public class AddAndActivateConnection2Tuple extends Tuple {

    @Position(0)
    private DBusPath path;
    @Position(1)
    private DBusPath activeConnection;
    @Position(2)
    private Map<String, Variant<?>> result;

    public AddAndActivateConnection2Tuple(DBusPath path, DBusPath activeConnection, Map<String, Variant<?>> result) {
        this.path = path;
        this.activeConnection = activeConnection;
        this.result = result;
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

    public void setResult(Map<String, Variant<?>> arg) {
        result = arg;
    }

    public Map<String, Variant<?>> getResult() {
        return result;
    }

}