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
    boolean isConfigurationValid(TritonServerServiceOptions options) {
        if (!options.isLocalEnabled()) {
            return !isNullOrEmpty(options.getAddress());
        }
        return !isNullOrEmpty(options.getBackendsPath()) && !isNullOrEmpty(options.getModelRepositoryPath());
    }

    @Override
    boolean isModelEncryptionEnabled(TritonServerServiceOptions options) {
        return options.isLocalEnabled() && options.isModelEncryptionPasswordSet();
    }
}
