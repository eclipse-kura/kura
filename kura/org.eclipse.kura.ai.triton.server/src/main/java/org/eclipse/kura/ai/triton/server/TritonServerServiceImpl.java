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

public class TritonServerServiceImpl extends TritonServerServiceAbs {

    @Override
    TritonServerInstanceManager createInstanceManager() {
        if (this.options.isLocalEnabled()) {
            return new TritonServerNativeManager(this.options, this.commandExecutorService, this.decryptionFolderPath);
        } else {
            return null;
        }
    }

    @Override
    boolean isConfigurationValid() {
        if (!this.options.isLocalEnabled()) {
            return !isNullOrEmpty(this.options.getAddress());
        }
        return !isNullOrEmpty(this.options.getBackendsPath()) && !isNullOrEmpty(this.options.getModelRepositoryPath());
    }
}
