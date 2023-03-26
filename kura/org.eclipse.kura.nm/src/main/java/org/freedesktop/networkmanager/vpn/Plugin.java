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
package org.freedesktop.networkmanager.vpn;

import java.util.List;
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
@DBusInterfaceName("org.freedesktop.NetworkManager.VPN.Plugin")
@DBusProperty(name = "State", type = UInt32.class, access = Access.READ)
public interface Plugin extends DBusInterface {

    public void Connect(Map<String, Map<String, Variant<?>>> connection);

    public void ConnectInteractive(Map<String, Map<String, Variant<?>>> connection, Map<String, Variant<?>> details);

    public String NeedSecrets(Map<String, Map<String, Variant<?>>> settings);

    public void Disconnect();

    public void SetConfig(Map<String, Variant<?>> config);

    public void SetIp4Config(Map<String, Variant<?>> config);

    public void SetIp6Config(Map<String, Variant<?>> config);

    public void SetFailure(String reason);

    public void NewSecrets(Map<String, Map<String, Variant<?>>> connection);

    public static class StateChanged extends DBusSignal {

        private final UInt32 state;

        public StateChanged(String _path, UInt32 _state) throws DBusException {
            super(_path, _state);
            this.state = _state;
        }

        public UInt32 getState() {
            return this.state;
        }

    }

    public static class SecretsRequired extends DBusSignal {

        private final String message;
        private final List<String> secrets;

        public SecretsRequired(String _path, String _message, List<String> _secrets) throws DBusException {
            super(_path, _message, _secrets);
            this.message = _message;
            this.secrets = _secrets;
        }

        public String getMessage() {
            return this.message;
        }

        public List<String> getSecrets() {
            return this.secrets;
        }

    }

    public static class Config extends DBusSignal {

        private final Map<String, Variant<?>> config;

        public Config(String _path, Map<String, Variant<?>> _config) throws DBusException {
            super(_path, _config);
            this.config = _config;
        }

        public Map<String, Variant<?>> getConfig() {
            return this.config;
        }

    }

    public static class Ip4Config extends DBusSignal {

        private final Map<String, Variant<?>> ip4config;

        public Ip4Config(String _path, Map<String, Variant<?>> _ip4config) throws DBusException {
            super(_path, _ip4config);
            this.ip4config = _ip4config;
        }

        public Map<String, Variant<?>> getIp4config() {
            return this.ip4config;
        }

    }

    public static class Ip6Config extends DBusSignal {

        private final Map<String, Variant<?>> ip6config;

        public Ip6Config(String _path, Map<String, Variant<?>> _ip6config) throws DBusException {
            super(_path, _ip6config);
            this.ip6config = _ip6config;
        }

        public Map<String, Variant<?>> getIp6config() {
            return this.ip6config;
        }

    }

    public static class LoginBanner extends DBusSignal {

        private final String banner;

        public LoginBanner(String _path, String _banner) throws DBusException {
            super(_path, _banner);
            this.banner = _banner;
        }

        public String getBanner() {
            return this.banner;
        }

    }

    public static class Failure extends DBusSignal {

        private final UInt32 reason;

        public Failure(String _path, UInt32 _reason) throws DBusException {
            super(_path, _reason);
            this.reason = _reason;
        }

        public UInt32 getReason() {
            return this.reason;
        }

    }
}
