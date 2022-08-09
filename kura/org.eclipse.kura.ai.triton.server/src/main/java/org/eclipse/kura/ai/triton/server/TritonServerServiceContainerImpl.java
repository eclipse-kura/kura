package org.eclipse.kura.ai.triton.server;

import org.eclipse.kura.container.orchestration.ContainerOrchestrationService;
import org.eclipse.kura.executor.CommandExecutorService;

public class TritonServerServiceContainerImpl extends TritonServerServiceAbs {

    @Override
    TritonServerInstanceManager createInstanceManager(TritonServerServiceOptions options,
            CommandExecutorService executorService, ContainerOrchestrationService orchestrationService,
            String decryptionFolderPath) {
        return new TritonServerContainerManager(options, orchestrationService, decryptionFolderPath);
    }

    @Override
    boolean isConfigurationValid() {
        return !isNullOrEmpty(this.options.getModelRepositoryPath()) && !isNullOrEmpty(this.options.getContainerImage())
                && !isNullOrEmpty(this.options.getContainerImageTag());
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
