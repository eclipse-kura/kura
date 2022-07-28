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