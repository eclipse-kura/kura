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
 ******************************************************************************/
package org.eclipse.kura.ai.triton.server;

import org.eclipse.kura.executor.CommandExecutorService;

public class TritonServerServiceRemoteImpl extends TritonServerServiceAbs {

    @Override
    TritonServerInstanceManager createInstanceManager(TritonServerServiceOptions options,
            CommandExecutorService executorService, String decryptionFolderPath) {
        return new TritonServerRemoteManager();
    }

    @Override
    boolean isConfigurationValid() {
        return !isNullOrEmpty(this.options.getAddress());
    }

    @Override
    boolean isModelEncryptionEnabled() {
        return false; // Feature not supported for remote instances
    }

    @Override
    String getServerAddress() {
        return this.options.getAddress();
    }
}