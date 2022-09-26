/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.event.publisher;


public final class EventPublisherConstants {

    private EventPublisherConstants() {
    }

    public static final String TOPIC_ACCOUNT_TOKEN = "#account-name";
    public static final String TOPIC_CLIENT_ID_TOKEN = "#client-id";
    public static final String TOPIC_SEPARATOR = "/";
    public static final String CONTROL = "CONTROL";
    public static final String FULL_TOPIC = "FULL_TOPIC";
    public static final String PRIORITY = "PRIORITY";
    public static final String QOS = "QOS";
    public static final String RETAIN = "RETAIN";

}
