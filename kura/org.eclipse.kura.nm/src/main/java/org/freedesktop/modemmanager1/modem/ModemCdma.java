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
package org.freedesktop.modemmanager1.modem;

import java.util.Map;

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
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.ModemCdma")
@DBusProperty(name = "ActivationState", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Meid", type = String.class, access = Access.READ)
@DBusProperty(name = "Esn", type = String.class, access = Access.READ)
@DBusProperty(name = "Sid", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Nid", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Cdma1xRegistrationState", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "EvdoRegistrationState", type = UInt32.class, access = Access.READ)
public interface ModemCdma extends DBusInterface {

    public void Activate(String carrierCode);

    public void ActivateManual(Map<String, Variant<?>> properties);

    public static class ActivationStateChanged extends DBusSignal {

        private final UInt32 activationState;
        private final UInt32 activationError;
        private final Map<String, Variant<?>> statusChanges;

        public ActivationStateChanged(String _path, UInt32 _activationState, UInt32 _activationError,
                Map<String, Variant<?>> _statusChanges) throws DBusException {
            super(_path, _activationState, _activationError, _statusChanges);
            this.activationState = _activationState;
            this.activationError = _activationError;
            this.statusChanges = _statusChanges;
        }

        public UInt32 getActivationState() {
            return this.activationState;
        }

        public UInt32 getActivationError() {
            return this.activationError;
        }

        public Map<String, Variant<?>> getStatusChanges() {
            return this.statusChanges;
        }

    }
}
