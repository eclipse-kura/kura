/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.devel.driver.dummy;

import java.util.Map;

import org.eclipse.kura.wire.devel.Property;

public class DummyDriverOptions {

    private static final Property<Integer> CONNECTION_DELAY = new Property<>("connection.delay", 0);
    private static final Property<String> CHANNEL_DESCRIPTOR_ISSUES = new Property<>("channel.descriptor.issues",
            ChannelDescriptorIssue.NONE.name());
    private static final Property<String> PREPARED_READ_ISSUES = new Property<>("prepared.read.issues",
            PreparedReadIssue.NONE.name());
    private static final Property<String> CONNECTION_ISSUES = new Property<>("connection.issues",
            ConnectionIssue.NONE.name());

    private final Map<String, Object> properties;

    public DummyDriverOptions(final Map<String, Object> properties) {
        this.properties = properties;
    }

    public int getConnectionDelay() {
        return CONNECTION_DELAY.get(properties);
    }

    public ChannelDescriptorIssue getChannelDescriptorIssues() {
        try {
            return ChannelDescriptorIssue.valueOf(CHANNEL_DESCRIPTOR_ISSUES.get(properties));
        } catch (Exception e) {
            return ChannelDescriptorIssue.NONE;
        }
    }

    public PreparedReadIssue getPreparedReadIssues() {
        try {
            return PreparedReadIssue.valueOf(PREPARED_READ_ISSUES.get(properties));
        } catch (Exception e) {
            return PreparedReadIssue.NONE;
        }
    }

    public ConnectionIssue getConnectionIssues() {
        try {
            return ConnectionIssue.valueOf(CONNECTION_ISSUES.get(properties));
        } catch (Exception e) {
            return ConnectionIssue.NONE;
        }
    }
}