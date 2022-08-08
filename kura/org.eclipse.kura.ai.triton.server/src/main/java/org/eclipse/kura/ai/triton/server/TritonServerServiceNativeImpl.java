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

import org.eclipse.kura.container.orchestration.ContainerOrchestrationService;
import org.eclipse.kura.executor.CommandExecutorService;

public class TritonServerServiceNativeImpl extends TritonServerServiceAbs {

    @Override
    TritonServerInstanceManager createInstanceManager(TritonServerServiceOptions options,
            CommandExecutorService executorService, ContainerOrchestrationService orchestrationService,
            String decryptionFolderPath) {
        return new TritonServerNativeManager(options, executorService, decryptionFolderPath);
    }

    @Override
    boolean isConfigurationValid() {
        return !isNullOrEmpty(this.options.getBackendsPath()) && !isNullOrEmpty(this.options.getModelRepositoryPath());
    }

    @Override
    boolean isModelEncryptionEnabled() {
        return this.options.isModelEncryptionPasswordSet();
    }

    @Override
    String getServerAddress() {
        return "localhost";
    }
}
