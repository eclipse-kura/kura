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
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.internal.rest.keystore.request.handler;


import org.osgi.service.component.annotations.Component;
@Component(
    name = "org.eclipse.kura.internal.rest.keystore.request.handler.KeystoreRequestHandlerV1",
    immediate = true,
    service = {},
    property = { "service.pid=org.eclipse.kura.internal.rest.keystore.request.handler.KeystoreServiceRequestHandlerV1" })
public class KeystoreServiceRequestHandlerV1 extends KeystoreServiceRequestHandler {

    public KeystoreServiceRequestHandlerV1() {
        super("KEYS-V1");
    }

}
