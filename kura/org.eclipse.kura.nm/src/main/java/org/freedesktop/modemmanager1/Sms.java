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

import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Sms")
@DBusProperty(name = "State", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "PduType", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Number", type = String.class, access = Access.READ)
@DBusProperty(name = "Text", type = String.class, access = Access.READ)
@DBusProperty(name = "Data", type = Sms.PropertyDataType.class, access = Access.READ)
@DBusProperty(name = "SMSC", type = String.class, access = Access.READ)
@DBusProperty(name = "Validity", type = PropertyValidityStruct.class, access = Access.READ)
@DBusProperty(name = "Class", type = Integer.class, access = Access.READ)
@DBusProperty(name = "TeleserviceId", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "ServiceCategory", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "DeliveryReportRequest", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "MessageReference", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Timestamp", type = String.class, access = Access.READ)
@DBusProperty(name = "DischargeTimestamp", type = String.class, access = Access.READ)
@DBusProperty(name = "DeliveryState", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Storage", type = UInt32.class, access = Access.READ)
public interface Sms extends DBusInterface {

    public void Send();

    public void Store(UInt32 storage);

    public static interface PropertyDataType extends TypeRef<List<Byte>> {

    }
}
