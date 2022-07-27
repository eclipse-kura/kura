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

public class TritonServerServiceOrigImpl extends TritonServerServiceAbs {

    @Override
    TritonServerInstanceManager createInstanceManager(TritonServerServiceOptions options,
            CommandExecutorService executorService, String decryptionFolderPath) {
        if (options.isLocalEnabled()) {
            return new TritonServerNativeManager(options, executorService, decryptionFolderPath);
        } else {
            return new TritonServerRemoteManager();
        }
    }

    @Override
    boolean isConfigurationValid() {
        if (!this.options.isLocalEnabled()) {
            return !isNullOrEmpty(this.options.getAddress());
        }
        return !isNullOrEmpty(this.options.getBackendsPath()) && !isNullOrEmpty(this.options.getModelRepositoryPath());
    }

    @Override
    boolean isModelEncryptionEnabled() {
        return this.options.isLocalEnabled() && this.options.isModelEncryptionPasswordSet();
    }
}
