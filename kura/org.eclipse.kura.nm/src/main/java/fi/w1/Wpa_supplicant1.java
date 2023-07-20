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
package fi.w1;

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
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("fi.w1.wpa_supplicant1")
@DBusProperty(name = "DebugLevel", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "DebugTimestamp", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "DebugShowKeys", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "Interfaces", type = Wpa_supplicant1.PropertyInterfacesType.class, access = Access.READ)
@DBusProperty(name = "EapMethods", type = Wpa_supplicant1.PropertyEapMethodsType.class, access = Access.READ)
@DBusProperty(name = "Capabilities", type = Wpa_supplicant1.PropertyCapabilitiesType.class, access = Access.READ)
@DBusProperty(name = "WFDIEs", type = Wpa_supplicant1.PropertyWFDIEsType.class, access = Access.READ_WRITE)
public interface Wpa_supplicant1 extends DBusInterface {

    public DBusPath CreateInterface(Map<String, Variant<?>> args);

    public void RemoveInterface(DBusPath path);

    public DBusPath GetInterface(String ifname);

    public void ExpectDisconnect();

    public static class InterfaceAdded extends DBusSignal {

        private final DBusPath addedInterfacePath;
        private final Map<String, Variant<?>> properties;

        public InterfaceAdded(String _path, DBusPath _addedInterfacePath, Map<String, Variant<?>> _properties)
                throws DBusException {
            super(_path, _addedInterfacePath, _properties);
            this.addedInterfacePath = _addedInterfacePath;
            this.properties = _properties;
        }

        public DBusPath getAddedInterfacePath() {
            return this.addedInterfacePath;
        }

        public Map<String, Variant<?>> getProperties() {
            return this.properties;
        }
    }

    public static class InterfaceRemoved extends DBusSignal {

        private final DBusPath removedInterfacePath;

        public InterfaceRemoved(String _path, DBusPath _removedInterfacePath) throws DBusException {
            super(_path, _removedInterfacePath);
            this.removedInterfacePath = _removedInterfacePath;
        }

        public DBusPath getRemovedInterfacePath() {
            return this.removedInterfacePath;
        }

    }

    public static class PropertiesChanged extends DBusSignal {

        private final Map<String, Variant<?>> properties;

        public PropertiesChanged(String _path, Map<String, Variant<?>> _properties) throws DBusException {
            super(_path, _properties);
            this.properties = _properties;
        }

        public Map<String, Variant<?>> getProperties() {
            return this.properties;
        }

    }

    public static interface PropertyInterfacesType extends TypeRef<List<DBusPath>> {

    }

    public static interface PropertyEapMethodsType extends TypeRef<List<String>> {

    }

    public static interface PropertyCapabilitiesType extends TypeRef<List<String>> {

    }

    public static interface PropertyWFDIEsType extends TypeRef<List<Byte>> {

    }
}
