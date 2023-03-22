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
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.NetworkManager.IP4Config")
@DBusProperty(name = "Addresses", type = IP4Config.PropertyAddressesType.class, access = Access.READ)
@DBusProperty(name = "AddressData", type = IP4Config.PropertyAddressDataType.class, access = Access.READ)
@DBusProperty(name = "Gateway", type = String.class, access = Access.READ)
@DBusProperty(name = "Routes", type = IP4Config.PropertyRoutesType.class, access = Access.READ)
@DBusProperty(name = "RouteData", type = IP4Config.PropertyRouteDataType.class, access = Access.READ)
@DBusProperty(name = "Nameservers", type = IP4Config.PropertyNameserversType.class, access = Access.READ)
@DBusProperty(name = "NameserverData", type = IP4Config.PropertyNameserverDataType.class, access = Access.READ)
@DBusProperty(name = "Domains", type = IP4Config.PropertyDomainsType.class, access = Access.READ)
@DBusProperty(name = "Searches", type = IP4Config.PropertySearchesType.class, access = Access.READ)
@DBusProperty(name = "DnsOptions", type = IP4Config.PropertyDnsOptionsType.class, access = Access.READ)
@DBusProperty(name = "DnsPriority", type = Integer.class, access = Access.READ)
@DBusProperty(name = "WinsServers", type = IP4Config.PropertyWinsServersType.class, access = Access.READ)
@DBusProperty(name = "WinsServerData", type = IP4Config.PropertyWinsServerDataType.class, access = Access.READ)
public interface IP4Config extends DBusInterface {

    public static interface PropertyAddressesType extends TypeRef<List<List<UInt32>>> {

    }

    public static interface PropertyAddressDataType extends TypeRef<List<Map<String, Variant>>> {

    }

    public static interface PropertyRoutesType extends TypeRef<List<List<UInt32>>> {

    }

    public static interface PropertyRouteDataType extends TypeRef<List<Map<String, Variant>>> {

    }

    public static interface PropertyNameserversType extends TypeRef<List<UInt32>> {

    }

    public static interface PropertyNameserverDataType extends TypeRef<List<Map<String, Variant>>> {

    }

    public static interface PropertyDomainsType extends TypeRef<List<String>> {

    }

    public static interface PropertySearchesType extends TypeRef<List<String>> {

    }

    public static interface PropertyDnsOptionsType extends TypeRef<List<String>> {

    }

    public static interface PropertyWinsServersType extends TypeRef<List<UInt32>> {

    }

    public static interface PropertyWinsServerDataType extends TypeRef<List<String>> {

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
