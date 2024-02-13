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

import java.util.Optional;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.container.orchestration.ImageInstanceDescriptor;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface representing a service for validating the signature of a container image
 *
 * @since 2.3
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ContainerSignatureValidationService {

    /**
     * Verifies the signature of a container image using the provided trust anchor. The trust anchor format depends on
     * the signature format. For example, if the signature was generated with Cosing, the trust anchor is a ECDSA public
     * key in PEM format. Other signature formats may require different trust anchors.
     *
     * If the signature is not included in a transparency log, the verification will fail unless the
     * verifyInTransparencyLog is set to false.
     *
     * If the image is signed with a different protocol the verification will fail.
     *
     * If the device running the verification has no internet access, the verification will fail.
     *
     * @param imageName
     *            The image name of the container image to verify. The value will need to be expressed in the form of
     *            registryURL/imagename in case of a custom registry.
     * @param imageReference
     *            The image tag (e.g. "latest") or the image digest (e.g. "sha256:xxxx") of the container image to verify.
     *            @warning For improved security, it is recommended to use the image digest as input.
     * @param trustAnchor
     *            The trust anchor to use for verification (e.g. a public key or a x509 certificate) typically in PEM
     *            format. The trust anchor is used to verify the signature of the container image.
     * @param verifyInTransparencyLog
     *            Sets the transparency log verification, to be used when an artifact signature has been uploaded to the
     *            transparency log. Artifacts cannot be publicly verified when not included in a log.
     * @param registryUsername
     *            Optional username for registry authentication. If the registry requires authentication,
     *            both username and password must be provided.
     * @param registryPassword
     *            Optional password for registry authentication. If the registry requires authentication,
     *            both username and password must be provided.
     * @return
     */
    public boolean verify(String imageName, String imageReference, String trustAnchor, boolean verifyInTransparencyLog,
            Optional<String> registryUsername, Optional<Password> registryPassword);

    /**
     * Verifies the signature of a container image using the provided trust anchor. The trust anchor format depends on
     * the signature format. For example, if the signature was generated with Cosing, the trust anchor is a ECDSA public
     * key in PEM format. Other signature formats may require different trust anchors.
     *
     * If the signature is not included in a transparency log, the verification will fail unless the
     * verifyInTransparencyLog is set to false.
     *
     * If the image is signed with a different protocol the verification will fail.
     *
     * If the device running the verification has no internet access, the verification will fail.
     *
     * @param imageDescriptor
     *            The image descriptor of the container image to verify (see {@link ImageInstanceDescriptor})
     * @param trustAnchor
     *            The trust anchor to use for verification (e.g. a public key or a x509 certificate) typically in PEM
     *            format. The trust anchor is used to verify the signature of the container image.
     * @param verifyInTransparencyLog
     *            Sets the transparency log verification, to be used when an artifact signature has been uploaded to the
     *            transparency log. Artifacts cannot be publicly verified when not included in a log.
     * @param registryUsername
     *            Optional username for registry authentication. If the registry requires authentication,
     *            both username and password must be provided.
     * @param registryPassword
     *            Optional password for registry authentication. If the registry requires authentication,
     *            both username and password must be provided.
     * @return
     */
    public boolean verify(ImageInstanceDescriptor imageDescriptor, String trustAnchor, boolean verifyInTransparencyLog,
            Optional<String> registryUsername, Optional<Password> registryPassword);
}
