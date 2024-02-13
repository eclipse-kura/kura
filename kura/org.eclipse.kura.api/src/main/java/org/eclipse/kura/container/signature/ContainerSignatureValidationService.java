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
     * @param containerImageDigest
     *            The digest of the container image to verify
     * @param trustAnchor
     *            The trust anchor to use for verification (e.g. a public key or a x509 certificate) typically in PEM
     *            format. The trust anchor is used to verify the signature of the container image.
     * @param werifyInTransparencyLog
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
    public boolean verify(String containerImageDigest, String trustAnchor, boolean verifyInTransparencyLog,
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
     * @warning For improved security, it is recommended to use the verify method that takes the image digest as input.
     *
     * @param imageName
     *            The image name of the container image to verify
     * @param imageTag
     *            The image tag of the container image to verify
     * @param trustAnchor
     *            The trust anchor to use for verification (e.g. a public key or a x509 certificate) typically in PEM
     *            format. The trust anchor is used to verify the signature of the container image.
     * @param werifyInTransparencyLog
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
    public boolean verify(String imageName, String imageTag, String publicKey, boolean verifyInTransparencyLog,
            Optional<String> registryUsername, Optional<Password> registryPassword);

}
