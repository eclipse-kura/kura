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

public class TritonServerServiceImpl extends TritonServerServiceAbs {

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
    boolean isConfigurationValid(TritonServerServiceOptions options) {
        if (!options.isLocalEnabled()) {
            return !isNullOrEmpty(options.getAddress());
        }
        return !isNullOrEmpty(options.getBackendsPath()) && !isNullOrEmpty(options.getModelRepositoryPath());
    }
}
