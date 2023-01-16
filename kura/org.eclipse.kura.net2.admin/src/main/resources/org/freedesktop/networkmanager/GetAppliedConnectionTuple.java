package org.freedesktop.networkmanager;

import java.util.Map;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public class GetAppliedConnectionTuple extends Tuple {

    @Position(0)
    private Map<String, Map<String, Variant<?>>> connection;
    @Position(1)
    private UInt64 versionId;

    public GetAppliedConnectionTuple(Map<String, Map<String, Variant<?>>> connection, UInt64 versionId) {
        this.connection = connection;
        this.versionId = versionId;
    }

    public void setConnection(Map<String, Map<String, Variant<?>>> arg) {
        connection = arg;
    }

    public Map<String, Map<String, Variant<?>>> getConnection() {
        return connection;
    }

    public void setVersionId(UInt64 arg) {
        versionId = arg;
    }

    public UInt64 getVersionId() {
        return versionId;
    }

}