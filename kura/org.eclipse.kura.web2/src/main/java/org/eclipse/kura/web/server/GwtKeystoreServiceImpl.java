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
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server;

import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.web.shared.service.GwtKeystoreService;

public class GwtKeystoreServiceImpl extends BaseGwtRestrictedComponentService implements GwtKeystoreService {

    /**
     * 
     */
    private static final long serialVersionUID = 7977086145487902679L;

    public GwtKeystoreServiceImpl() {
        super(KeystoreService.class);
    }
}
