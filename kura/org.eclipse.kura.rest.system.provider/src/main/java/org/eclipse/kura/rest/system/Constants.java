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
 ******************************************************************************/

package org.eclipse.kura.rest.system;


public class Constants {

    public static final String MQTT_APP_ID = "SYS-V1";
    public static final String REST_APP_ID = "system/v1";
    public static final String RESOURCE_FRAMEWORK_PROPERTIES = "/properties/framework";
    public static final String RESOURCE_FRAMEWORK_PROPERTIES_FILTER = "/properties/framework/filter";
    public static final String RESOURCE_EXTENDED_PROPERTIES = "/properties/extended";
    public static final String RESOURCE_EXTENDED_PROPERTIES_FILTER = "/properties/extended/filter";
    public static final String RESOURCE_KURA_PROPERTIES = "/properties/kura";
    public static final String RESOURCE_KURA_PROPERTIES_FILTER = "/properties/kura/filter";
    public static final String REST_ROLE_NAME = "system";
    public static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private Constants() {
    }

}
