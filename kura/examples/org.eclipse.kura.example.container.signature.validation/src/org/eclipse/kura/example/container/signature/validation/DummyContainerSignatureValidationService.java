package org.eclipse.kura.example.container.signature.validation;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.container.orchestration.ImageInstanceDescriptor;
import org.eclipse.kura.container.signature.ContainerSignatureValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyContainerSignatureValidationService
        implements ContainerSignatureValidationService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(DummyContainerSignatureValidationService.class);
    private boolean validationResult = false;

    protected void activate(Map<String, Object> properties) {
        logger.info("Activate DummyContainerSignatureValidationService...");
        updated(properties);
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Update DummyContainerSignatureValidationService...");

        // WIP
        this.validationResult = (boolean) properties.get("manual.setValidationOutcome");
    }

    protected void deactivate() {
        logger.info("Deactivate DummyContainerSignatureValidationService...");
    }

    @Override
    public boolean verify(String imageName, String imageReference, String trustAnchor, boolean verifyInTransparencyLog)
            throws KuraException {
        logger.info("Validating signature for {}:{}", imageName, imageReference);
        return this.validationResult;
    }

    @Override
    public boolean verify(String imageName, String imageReference, String trustAnchor, boolean verifyInTransparencyLog,
            String registryUsername, Password registryPassword) throws KuraException {
        logger.info("Validating signature for {}:{} using authenticated registry", imageName, imageReference);
        return this.validationResult;
    }

    @Override
    public boolean verify(ImageInstanceDescriptor imageDescriptor, String trustAnchor, boolean verifyInTransparencyLog)
            throws KuraException {
        logger.info("Validating signature for {}:{}", imageDescriptor.getImageName(), imageDescriptor.getImageId());
        return this.validationResult;
    }

    @Override
    public boolean verify(ImageInstanceDescriptor imageDescriptor, String trustAnchor, boolean verifyInTransparencyLog,
            String registryUsername, Password registryPassword) throws KuraException {
        logger.info("Validating signature for {}:{} using authenticated registry", imageDescriptor.getImageName(),
                imageDescriptor.getImageId());
        return this.validationResult;
    }

}
