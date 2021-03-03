/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Eurotech
 ******************************************************************************/
package org.eclipse.kura.internal.floodingprotection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;

public class FloodingProtectionOptions {

    private static final String[] FLOODING_PROTECTION_MANGLE_RULES = {
            "-A prerouting-kura -m conntrack --ctstate INVALID -j DROP",
            "-A prerouting-kura -p tcp ! --syn -m conntrack --ctstate NEW -j DROP",
            "-A prerouting-kura -p tcp -m conntrack --ctstate NEW -m tcpmss ! --mss 536:65535 -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags FIN,SYN FIN,SYN -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags SYN,RST SYN,RST -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags FIN,RST FIN,RST -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags FIN,ACK FIN -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ACK,URG URG -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ACK,FIN FIN -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ACK,PSH PSH -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL ALL -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL NONE -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL FIN,PSH,URG -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL SYN,FIN,PSH,URG -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL SYN,RST,ACK,FIN,URG -j DROP",
            "-A prerouting-kura -p icmp -j DROP", "-A prerouting-kura -f -j DROP" };

    private static final String PID = "org.eclipse.kura.internal.floodingprotection.FloodingProtectionConfigurator";
    private static final String FP_DESCRIPTION = "The service enables flooding protection mechanisms via iptables.";
    private static final String FP_ENABLED_PROP_NAME = "flooding.protection.enabled";
    private static final String FP_ENABLED_DESCRIPTION = "Enable the flooding protection feature.";
    private static final boolean FP_ENABLED_DEFAULT = false;

    private Map<String, Object> properties = new HashMap<>();

    public FloodingProtectionOptions(Map<String, Object> properties) {
        setProperties(properties);
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties.clear();
        this.properties.put(FP_ENABLED_PROP_NAME, properties.getOrDefault(FP_ENABLED_PROP_NAME, FP_ENABLED_DEFAULT));
    }

    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public String getPid() {
        return PID;
    }

    public Set<String> getFloodingProtectionFilterRules() {
        return new HashSet<>();
    }

    public Set<String> getFloodingProtectionNatRules() {
        return new HashSet<>();
    }

    public Set<String> getFloodingProtectionMangleRules() {
        if ((boolean) this.properties.get(FP_ENABLED_PROP_NAME)) {
            return new HashSet<>(Arrays.asList(FLOODING_PROTECTION_MANGLE_RULES));
        } else {
            return new HashSet<>();
        }
    }

    public Tocd getDefinition() {
        ObjectFactory objectFactory = new ObjectFactory();
        Tocd tocd = objectFactory.createTocd();
        tocd.setName("Flooding Protection Service");
        tocd.setId(PID);
        tocd.setDescription(FP_DESCRIPTION);

        Tad tadEnabled = objectFactory.createTad();
        tadEnabled.setId(FP_ENABLED_PROP_NAME);
        tadEnabled.setName(FP_ENABLED_PROP_NAME);
        tadEnabled.setType(Tscalar.BOOLEAN);
        tadEnabled.setRequired(true);
        tadEnabled.setDefault(Boolean.toString(FP_ENABLED_DEFAULT));
        tadEnabled.setDescription(FP_ENABLED_DESCRIPTION);
        tocd.addAD(tadEnabled);

        return tocd;
    }
}
