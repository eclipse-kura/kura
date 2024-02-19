package org.eclipse.kura.container.signature;

import java.util.Objects;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Class representing the result of the signature validation performed by {@link:ContainerSignatureValidationService}
 * 
 * The result of the validation is composed of two main parts: whether or not the container image signature was
 * validated and the container image digest (in the "algorithm:encoded" format, @see
 * <a href="https://github.com/opencontainers/image-spec/blob/main/descriptor.md#digests">Opencontainers specs</a>)
 *
 * If the signature is valid, the image digest MUST be provided.
 *
 * @since 2.7
 */
@ProviderType
public final class ValidationResult {

    private boolean isSignatureValid = false;
    private Optional<String> imageDigest = Optional.empty();

    public ValidationResult(boolean signatureValid, Optional<String> digest) {
        this.imageDigest = Objects.requireNonNull(digest);
        this.isSignatureValid = Objects.requireNonNull(signatureValid);

        if (this.isSignatureValid && (!this.imageDigest.isPresent() || this.imageDigest.get().isEmpty())) {
            throw new IllegalArgumentException("Image digest must be provided when signature is valid.");
        }
    }

    public boolean isSignatureValid() {
        return this.isSignatureValid;
    }

    public Optional<String> imageDigest() {
        return this.imageDigest;
    }

}
