/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.security.provider;

import javax.ws.rs.Path;

@Path("security/v1")
public class SecurityRestServiceV1 extends AbstractRestSecurityService {

    private static final String MQTT_APP_ID = "SEC-V1";

    @Override
    public String getMqttAppId() {
        return MQTT_APP_ID;
    }
}
