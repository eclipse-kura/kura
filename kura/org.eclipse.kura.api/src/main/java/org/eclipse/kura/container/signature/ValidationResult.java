/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.container.signature;

import java.util.Objects;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Class representing the result of the signature validation performed by {@link:ContainerSignatureValidationService}
 * 
 * The validation result is composed of two main parts: whether or not the container image signature was
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

    public ValidationResult() {
        // Nothing to do
    }

    public ValidationResult(boolean signatureValid, String digest) {
        if (Objects.isNull(signatureValid) || Objects.isNull(digest)) {
            throw new NullPointerException("Signature results and digest cannot be null.");
        }

        if (signatureValid && digest.isEmpty()) {
            throw new IllegalArgumentException("Image digest must be provided when signature is valid.");
        }

        this.imageDigest = Optional.of(digest);
        this.isSignatureValid = signatureValid;
    }

    public boolean isSignatureValid() {
        return this.isSignatureValid;
    }

    public Optional<String> imageDigest() {
        return this.imageDigest;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ValidationResult other = (ValidationResult) obj;
        return this.isSignatureValid == other.isSignatureValid && this.imageDigest.equals(other.imageDigest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.isSignatureValid, this.imageDigest);
    }
}
