/*******************************************************************************
 * Copyright (c) 2023, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.keystore.provider;

import jakarta.ws.rs.Path;

import org.osgi.service.component.annotations.Component;
@Path("/keystores/v1")
@Component(
    name = "org.eclipse.kura.internal.rest.keystore.provider.KeystoreRestServiceV1",
    immediate = true,
    service = { org.eclipse.kura.internal.rest.keystore.provider.KeystoreRestService.class },
    property = {
        "osgi.jakartars.resource=true" })
public class KeystoreRestServiceV1 extends KeystoreRestService {

}
