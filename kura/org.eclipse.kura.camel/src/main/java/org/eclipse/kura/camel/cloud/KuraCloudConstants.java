/*******************************************************************************
 * Copyright (c) 2011, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.camel.cloud;

/**
 * Kura cloud constants
 */
public final class KuraCloudConstants {

    public static final String TOPIC = "topic";
    public static final String APPLICATION_ID = "applicationId";

    public static final String CAMEL_KURA_CLOUD = "CamelKuraCloud";
    public static final String CAMEL_KURA_CLOUD_TOPIC = CAMEL_KURA_CLOUD + ".topic";
    public static final String CAMEL_KURA_CLOUD_QOS = CAMEL_KURA_CLOUD + ".qos";
    public static final String CAMEL_KURA_CLOUD_RETAIN = CAMEL_KURA_CLOUD + ".retain";
    public static final String CAMEL_KURA_CLOUD_PRIORITY = CAMEL_KURA_CLOUD + ".priority";
    public static final String CAMEL_KURA_CLOUD_CONTROL = CAMEL_KURA_CLOUD + ".control";
    public static final String CAMEL_KURA_CLOUD_DEVICEID = CAMEL_KURA_CLOUD + ".deviceId";

    private KuraCloudConstants() {
    }

}
