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
@DBusInterfaceName("org.freedesktop.ModemManager1.Call")
@DBusProperty(name = "State", type = Integer.class, access = Access.READ)
@DBusProperty(name = "StateReason", type = Integer.class, access = Access.READ)
@DBusProperty(name = "Direction", type = Integer.class, access = Access.READ)
@DBusProperty(name = "Number", type = String.class, access = Access.READ)
@DBusProperty(name = "Multiparty", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "AudioPort", type = String.class, access = Access.READ)
@DBusProperty(name = "AudioFormat", type = Call.PropertyAudioFormatType.class, access = Access.READ)
public interface Call extends DBusInterface {

    public void Start();

    public void Accept();

    public void Deflect(String number);

    public void JoinMultiparty();

    public void LeaveMultiparty();

    public void Hangup();

    public void SendDtmf(String dtmf);

    public static class DtmfReceived extends DBusSignal {

        private final String dtmf;

        public DtmfReceived(String _path, String _dtmf) throws DBusException {
            super(_path, _dtmf);
            this.dtmf = _dtmf;
        }

        public String getDtmf() {
            return this.dtmf;
        }

    }

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

    public static interface PropertyAudioFormatType extends TypeRef<Map<String, Variant>> {

    }
}
