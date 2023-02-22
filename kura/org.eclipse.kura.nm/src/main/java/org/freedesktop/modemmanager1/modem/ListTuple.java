package org.freedesktop.modemmanager1.modem;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class ListTuple extends Tuple {
    @Position(0)
    private String selected;
    @Position(1)
    private List<Map<String, Variant<?>>> installed;

    public ListTuple(String selected, List<Map<String, Variant<?>>> installed) {
        this.selected = selected;
        this.installed = installed;
    }

    public void setSelected(String arg) {
        selected = arg;
    }

    public String getSelected() {
        return selected;
    }
    public void setInstalled(List<Map<String, Variant<?>>> arg) {
        installed = arg;
    }

    public List<Map<String, Variant<?>>> getInstalled() {
        return installed;
    }


}