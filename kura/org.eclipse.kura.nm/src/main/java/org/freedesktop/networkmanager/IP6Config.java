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
package org.freedesktop.networkmanager;

import java.util.List;
import java.util.Map;

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
@DBusInterfaceName("org.freedesktop.NetworkManager.IP6Config")
@DBusProperty(name = "Addresses", type = IP6Config.PropertyAddressesType.class, access = Access.READ)
@DBusProperty(name = "AddressData", type = IP6Config.PropertyAddressDataType.class, access = Access.READ)
@DBusProperty(name = "Gateway", type = String.class, access = Access.READ)
@DBusProperty(name = "Routes", type = IP6Config.PropertyRoutesType.class, access = Access.READ)
@DBusProperty(name = "RouteData", type = IP6Config.PropertyRouteDataType.class, access = Access.READ)
@DBusProperty(name = "Nameservers", type = IP6Config.PropertyNameserversType.class, access = Access.READ)
@DBusProperty(name = "Domains", type = IP6Config.PropertyDomainsType.class, access = Access.READ)
@DBusProperty(name = "Searches", type = IP6Config.PropertySearchesType.class, access = Access.READ)
@DBusProperty(name = "DnsOptions", type = IP6Config.PropertyDnsOptionsType.class, access = Access.READ)
@DBusProperty(name = "DnsPriority", type = Integer.class, access = Access.READ)
public interface IP6Config extends DBusInterface {

    public static interface PropertyAddressesType extends TypeRef<List<PropertyAddressesStruct>> {

    }

    public static interface PropertyAddressDataType extends TypeRef<List<Map<String, Variant>>> {

    }

    public static interface PropertyRoutesType extends TypeRef<List<PropertyRoutesStruct>> {

    }

    public static interface PropertyRouteDataType extends TypeRef<List<Map<String, Variant>>> {

    }

    public static interface PropertyNameserversType extends TypeRef<List<List<Byte>>> {

    }

    public static interface PropertyDomainsType extends TypeRef<List<String>> {

    }

    public static interface PropertySearchesType extends TypeRef<List<String>> {

    }

    public static interface PropertyDnsOptionsType extends TypeRef<List<String>> {

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
}
