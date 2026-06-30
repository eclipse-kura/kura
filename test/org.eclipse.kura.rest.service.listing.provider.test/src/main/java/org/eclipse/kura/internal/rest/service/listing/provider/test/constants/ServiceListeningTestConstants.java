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
package org.eclipse.kura.internal.rest.service.listing.provider.test.constants;

public class ServiceListeningTestConstants {

    /*
     * POST BODIES
     */

    public static final String COMPLETE_POST_BODY = "{\"interfaceNames\": [ \"org.eclipse.kura.configuration.ConfigurableComponent\", \"org.eclipse.kura.security.keystore.KeystoreService\"]}";
    public static final String NULL_POST_BODY = "{}";
    public static final String EMPTY_POST_BODY = "{\"interfaceNames\": []}";
    public static final String NULL_FIELD_POST_BODY = "{\"interfaceNames\": [\"org.eclipse.kura.configuration.ConfigurableComponent\",]}";
    public static final String EMPTY_FIELD_POST_BODY = "{\"interfaceNames\": [\"org.eclipse.kura.configuration.ConfigurableComponent\",\"\"]}";

    /*
     * END POINTS
     */

    

    /*
     * CORRECT RESPONSES
     */

    public static final String FILTERED_SERVICES_RESPONSE = "{\"pids\":[\"SSLKeystore\"]}";

    /*
     * FAILING MESSAGES ON FAILING
     */

    public static final String NULL_BODY_RESPONSE = "{\"message\":\"Bad request. interfaceNames cannot be null\"}";
    public static final String EMPTY_BODY_RESPONSE = "{\"message\":\"Bad request. interfaceNames cannot be empty\"}";
    public static final String NULL_FIELD_BODY_RESPONSE = "{\"message\":\"Bad request. interfaceNames elements cannot be null\"}";
    public static final String EMPTY_FIELD_BODY_RESPONSE = "{\"message\":\"Bad request. interfaceNames elements cannot be empty\"}";

    private ServiceListeningTestConstants() {
    }
}
