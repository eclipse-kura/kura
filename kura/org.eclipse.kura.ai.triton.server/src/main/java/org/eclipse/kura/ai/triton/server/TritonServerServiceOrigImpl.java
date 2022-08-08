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

/**
 * @deprecated since version 1.1 in favor of
 *             {@link org.eclipse.kura.ai.triton.server.TritonServerRemoteServiceImpl} and
 *             {@link org.eclipse.kura.ai.triton.server.TritonServerNativeServiceImpl}.
 */
@Deprecated
public class TritonServerServiceOrigImpl extends TritonServerServiceAbs {

    @Override
    TritonServerInstanceManager createInstanceManager(TritonServerServiceOptions options,
            CommandExecutorService executorService, ContainerOrchestrationService orchestrationService,
            String decryptionFolderPath) {
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

    @Override
    String getServerAddress() {
        if (this.options.isLocalEnabled()) {
            return "localhost";
        } else {
            return this.options.getAddress();
        }
    }
}
