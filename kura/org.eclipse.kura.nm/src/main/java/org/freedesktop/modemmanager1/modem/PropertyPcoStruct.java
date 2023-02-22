package org.freedesktop.modemmanager1.modem;

import java.util.List;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class PropertyPcoStruct extends Struct {
    @Position(0)
    private final UInt32 member0;
    @Position(1)
    private final boolean member1;
    @Position(2)
    private final List<Byte> member2;

    public PropertyPcoStruct(UInt32 member0, boolean member1, List<Byte> member2) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
    }


    public UInt32 getMember0() {
        return member0;
    }

    public boolean getMember1() {
        return member1;
    }

    public List<Byte> getMember2() {
        return member2;
    }


}
