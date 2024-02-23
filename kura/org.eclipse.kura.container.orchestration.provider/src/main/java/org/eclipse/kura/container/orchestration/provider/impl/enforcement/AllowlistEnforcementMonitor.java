package org.eclipse.kura.container.orchestration.provider.impl.enforcement;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.container.orchestration.provider.impl.ContainerOrchestrationServiceImpl;
import org.eclipse.kura.container.orchestration.provider.impl.ContainerOrchestrationServiceOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Event;

public class AllowlistEnforcementMonitor extends ResultCallbackTemplate<AllowlistEnforcementMonitor, Event> {

    private static final Logger logger = LoggerFactory.getLogger(AllowlistEnforcementMonitor.class);
    private static final String ENFORCEMENT_SUCCESS = "Enforcement allowlist contains image digests {}...container {} is starting";
    private static final String ENFORCEMENT_FAILURE = "Enforcement allowlist doesn't contain image digests...container {} will be stopped";
    private final ContainerOrchestrationServiceOptions currentConfig;
    private final ContainerOrchestrationServiceImpl orchestrationServiceImpl;

    public AllowlistEnforcementMonitor(ContainerOrchestrationServiceOptions currentConfiguration,
            ContainerOrchestrationServiceImpl containerOrchestrationService) {

        this.currentConfig = currentConfiguration;
        this.orchestrationServiceImpl = containerOrchestrationService;
    }

    @Override
    public void onNext(Event item) {
        try {
            implementAllowlistEnforcement(item.getId(), currentConfig);
        } catch (KuraException e) {
            logger.error("Error during container stopping process");
        }
    }

    private void implementAllowlistEnforcement(String id, ContainerOrchestrationServiceOptions currentConfig)
            throws KuraException {
        List<String> digestsList = this.orchestrationServiceImpl
                .getImageDigestsByContainerName(getContainerNameById(id));

        List<String> digestIntersection = currentConfig.getEnforcementAllowlistContent().stream().distinct()
                .filter(digestsList::contains).collect(Collectors.toList());

        if (!digestIntersection.isEmpty()) {
            logger.info(ENFORCEMENT_SUCCESS, digestIntersection, id);
        } else {
            logger.error(ENFORCEMENT_FAILURE, id);
            this.orchestrationServiceImpl.stopContainer(id);
            this.orchestrationServiceImpl.deleteContainer(id);
        }
    }

    private String getContainerNameById(String id) {
        return this.orchestrationServiceImpl.listContainerDescriptors().stream()
                .filter(container -> container.getContainerId().equals(id)).findFirst()
                .map(container -> container.getContainerName()).orElse(null);
    }

}
