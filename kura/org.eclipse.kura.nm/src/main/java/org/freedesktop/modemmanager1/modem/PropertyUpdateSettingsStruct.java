package org.freedesktop.modemmanager1.modem;

import java.util.Map;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public class PropertyUpdateSettingsStruct extends Struct {
    @Position(0)
    private final UInt32 member0;
    @Position(1)
    private final Map<String, Variant<?>> member1;

    public PropertyUpdateSettingsStruct(UInt32 member0, Map<String, Variant<?>> member1) {
        this.member0 = member0;
        this.member1 = member1;
    }


    public UInt32 getMember0() {
        return member0;
    }

    public Map<String, Variant<?>> getMember1() {
        return member1;
    }


}