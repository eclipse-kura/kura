package org.freedesktop.modemmanager1;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class PropertyPortsStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final UInt32 member1;

    public PropertyPortsStruct(String member0, UInt32 member1) {
        this.member0 = member0;
        this.member1 = member1;
    }


    public String getMember0() {
        return member0;
    }

    public UInt32 getMember1() {
        return member1;
    }


}