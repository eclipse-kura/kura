package org.eclipse.kura.ai.triton.server;

import org.eclipse.kura.executor.CommandExecutorService;

public class TritonServerServiceNativeImpl extends TritonServerServiceAbs {

    @Override
    TritonServerInstanceManager createInstanceManager(TritonServerServiceOptions options,
            CommandExecutorService executorService, String decryptionFolderPath) {
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
