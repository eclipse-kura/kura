package org.freedesktop.networkmanager;

import java.util.List;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class PropertyAddressesStruct extends Struct {

    @Position(0)
    private final List<Byte> member0;
    @Position(1)
    private final UInt32 member1;
    @Position(2)
    private final List<Byte> member2;

    public PropertyAddressesStruct(List<Byte> member0, UInt32 member1, List<Byte> member2) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
    }

    public List<Byte> getMember0() {
        return member0;
    }

    public UInt32 getMember1() {
        return member1;
    }

    public List<Byte> getMember2() {
        return member2;
    }

}