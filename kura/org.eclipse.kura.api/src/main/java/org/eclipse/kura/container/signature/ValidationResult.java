package org.eclipse.kura.container.signature;

import java.util.Optional;

/**
 * Class representing the result of the signature validation performed by {@link:ContainerSignatureValidationService}
 * 
 * The result of the validation is composed of two main parts: whether or not the container image signature was
 * validated and the container image digest (in the "shaXXX:YYY" format).
 *
 * @since 2.7
 */
@ProviderType
public final class ValidationResult {

    private boolean isSignatureValid = false;
    private Optional<String> imageDigest = Optional.empty();

    public ValidationResult(boolean signatureValid, Optional<String> digest) {
        this.imageDigest = digest;
        this.isSignatureValid = signatureValid;
    }

    public boolean isSignatureValid() {
        return this.isSignatureValid;
    }

    public Optional<String> imageDigest() {
        return this.imageDigest;
    }

}