package org.freedesktop;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class GetLoggingTuple extends Tuple {
    @Position(0)
    private String level;
    @Position(1)
    private String domains;

    public GetLoggingTuple(String level, String domains) {
        this.level = level;
        this.domains = domains;
    }

    public void setLevel(String arg) {
        level = arg;
    }

    public String getLevel() {
        return level;
    }
    public void setDomains(String arg) {
        domains = arg;
    }

    public String getDomains() {
        return domains;
    }


}