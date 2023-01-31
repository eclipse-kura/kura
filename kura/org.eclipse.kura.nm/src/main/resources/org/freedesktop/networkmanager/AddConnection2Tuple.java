package org.freedesktop.networkmanager;

import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public class AddConnection2Tuple extends Tuple {

    @Position(0)
    private DBusPath path;
    @Position(1)
    private Map<String, Variant<?>> result;

    public AddConnection2Tuple(DBusPath path, Map<String, Variant<?>> result) {
        this.path = path;
        this.result = result;
    }

    public void setPath(DBusPath arg) {
        path = arg;
    }

    public DBusPath getPath() {
        return path;
    }

    public void setResult(Map<String, Variant<?>> arg) {
        result = arg;
    }

    public Map<String, Variant<?>> getResult() {
        return result;
    }

}