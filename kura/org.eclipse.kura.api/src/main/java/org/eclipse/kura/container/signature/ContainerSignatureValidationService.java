package org.eclipse.kura.container.signature;

import java.util.Optional;
import org.eclipse.kura.configuration.Password;

public interface ContainerSignatureValidationService {

    public boolean verify(String containerImageDigest, String publicKey, boolean verifyInTransparencyLog,
            Optional<String> registryUsername, Optional<Password> registryPassword);

    public boolean verify(String imageName, String imageTag, String publicKey, boolean verifyInTransparencyLog,
            Optional<String> registryUsername, Optional<Password> registryPassword);

}