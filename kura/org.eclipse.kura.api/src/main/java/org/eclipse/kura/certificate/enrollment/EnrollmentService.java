/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.certificate.enrollment;

import java.security.cert.CertStore;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Provides a list of APIs allowing the system to perform an enrollment in a Certificate Authority
 *
 * @since 2.4
 */

@ProviderType
public interface EnrollmentService {

    /**
     * 
     * Perform the enrollment of the system with a configured Certificate Authority.
     * 
     * @throws KuraException
     *             if it is impossible to enroll the system.
     */
    public void enroll() throws KuraException;

    /**
     * Renews the client certificate creating a new keypair and a new CSR submitted to the CA.
     * 
     * @throws KuraException
     *             it it is impossible to renew the system certificate
     */
    public void renew() throws KuraException;

    /**
     * 
     * Get the list of the Certificate Authority certificates stored in the relative
     * {@link org.eclipse.kura.security.keystore.KeystoreService}
     * 
     * @return returns the Certificate Authority <code>CertStore</code>
     * @throws KuraException
     *             if it is impossible to retrieve the certificate.
     */
    public CertStore getCACertificate() throws KuraException;

    /**
     * Get the list of Client certificates stored in the relative
     * {@link org.eclipse.kura.security.keystore.KeystoreService}
     * 
     * @return returns the Client <code>CertStore</code> containing the certificates chain obtained by the Certificate
     *         Authority
     * @throws KuraException
     *             if it is impossible to retrieve the certificate.
     */
    public CertStore getClientCertificate() throws KuraException;

    /**
     * Force the update of Certificate Authority certificate if a newest is available.
     * 
     * @throws KuraException
     *             if it is impossible to perform the request to the server.
     */
    public void forceCACertificateRollover() throws KuraException;

    /**
     * 
     * Check if the system in enrolled or not, that is the presence or not of the client certificate.
     * 
     * @return true if the system in enrolled, false otherwise.
     * @throws KuraException
     *             if it is impossible to query the keystore for the certificate availability.
     */
    public boolean isEnrolled() throws KuraException;

}
