/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.freedesktop.modemmanager1;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem")
@DBusProperty(name = "Sim", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "Bearers", type = Modem.PropertyBearersType.class, access = Access.READ)
@DBusProperty(name = "SupportedCapabilities", type = Modem.PropertySupportedCapabilitiesType.class, access = Access.READ)
@DBusProperty(name = "CurrentCapabilities", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "MaxBearers", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "MaxActiveBearers", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Manufacturer", type = String.class, access = Access.READ)
@DBusProperty(name = "Model", type = String.class, access = Access.READ)
@DBusProperty(name = "Revision", type = String.class, access = Access.READ)
@DBusProperty(name = "CarrierConfiguration", type = String.class, access = Access.READ)
@DBusProperty(name = "CarrierConfigurationRevision", type = String.class, access = Access.READ)
@DBusProperty(name = "HardwareRevision", type = String.class, access = Access.READ)
@DBusProperty(name = "DeviceIdentifier", type = String.class, access = Access.READ)
@DBusProperty(name = "Device", type = String.class, access = Access.READ)
@DBusProperty(name = "Drivers", type = Modem.PropertyDriversType.class, access = Access.READ)
@DBusProperty(name = "Plugin", type = String.class, access = Access.READ)
@DBusProperty(name = "PrimaryPort", type = String.class, access = Access.READ)
@DBusProperty(name = "Ports", type = Modem.PropertyPortsType.class, access = Access.READ)
@DBusProperty(name = "EquipmentIdentifier", type = String.class, access = Access.READ)
@DBusProperty(name = "UnlockRequired", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "UnlockRetries", type = Modem.PropertyUnlockRetriesType.class, access = Access.READ)
@DBusProperty(name = "State", type = Integer.class, access = Access.READ)
@DBusProperty(name = "StateFailedReason", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "AccessTechnologies", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "SignalQuality", type = PropertySignalQualityStruct.class, access = Access.READ)
@DBusProperty(name = "OwnNumbers", type = Modem.PropertyOwnNumbersType.class, access = Access.READ)
@DBusProperty(name = "PowerState", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "SupportedModes", type = Modem.PropertySupportedModesType.class, access = Access.READ)
@DBusProperty(name = "CurrentModes", type = PropertyCurrentModesStruct.class, access = Access.READ)
@DBusProperty(name = "SupportedBands", type = Modem.PropertySupportedBandsType.class, access = Access.READ)
@DBusProperty(name = "CurrentBands", type = Modem.PropertyCurrentBandsType.class, access = Access.READ)
@DBusProperty(name = "SupportedIpFamilies", type = UInt32.class, access = Access.READ)
public interface Modem extends DBusInterface {

    public void Enable(boolean enable);

    public List<DBusPath> ListBearers();

    public DBusPath CreateBearer(Map<String, Variant<?>> properties);

    public void DeleteBearer(DBusPath bearer);

    public void Reset();

    public void FactoryReset(String code);

    public void SetPowerState(UInt32 state);

    public void SetCurrentCapabilities(UInt32 capabilities);

    public void SetCurrentModes(SetCurrentModesStruct modes);

    public void SetCurrentBands(List<UInt32> bands);

    public String Command(String cmd, UInt32 timeout);

    public static class StateChanged extends DBusSignal {

        private final int old;
        private final int newparam;
        private final UInt32 reason;

        public StateChanged(String _path, int _old, int _new, UInt32 _reason) throws DBusException {
            super(_path, _old, _new, _reason);
            this.old = _old;
            this.newparam = _new;
            this.reason = _reason;
        }

        public int getOld() {
            return this.old;
        }

        public int getNewparam() {
            return this.newparam;
        }

        public UInt32 getReason() {
            return this.reason;
        }

    }

    public static interface PropertyBearersType extends TypeRef<List<DBusPath>> {

    }

    public static interface PropertySupportedCapabilitiesType extends TypeRef<List<UInt32>> {

    }

    public static interface PropertyDriversType extends TypeRef<List<String>> {

    }

    public static interface PropertyPortsType extends TypeRef<List<PropertyPortsStruct>> {

    }

    public static interface PropertyUnlockRetriesType extends TypeRef<Map<UInt32, UInt32>> {

    }

    public static interface PropertyOwnNumbersType extends TypeRef<List<String>> {

    }

    public static interface PropertySupportedModesType extends TypeRef<List<PropertySupportedModesStruct>> {

    }

    public static interface PropertySupportedBandsType extends TypeRef<List<UInt32>> {

    }

    public static interface PropertyCurrentBandsType extends TypeRef<List<UInt32>> {

    }
}
