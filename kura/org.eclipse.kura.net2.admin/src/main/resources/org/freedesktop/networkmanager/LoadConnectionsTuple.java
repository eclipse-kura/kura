package org.freedesktop.networkmanager;

import java.util.List;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class LoadConnectionsTuple extends Tuple {

    @Position(0)
    private boolean status;
    @Position(1)
    private List<String> failures;

    public LoadConnectionsTuple(boolean status, List<String> failures) {
        this.status = status;
        this.failures = failures;
    }

    public void setStatus(boolean arg) {
        status = arg;
    }

    public boolean getStatus() {
        return status;
    }

    public void setFailures(List<String> arg) {
        failures = arg;
    }

    public List<String> getFailures() {
        return failures;
    }

}