/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin.visitor.linux.util;

/**
 * Defines PAP authentication in Linux
 */
public class PapLinux extends PppAuthSecrets {

    /*
     * #Secrets for authentication using PAP
     * #client server secret IP addresses Provider
     * ISP@CINGULARGPRS.COM * CINGULAR1 * #att
     * mobileweb * password * #o2
     * user * pass * #orange
     * web * web * #vodaphone
     */

    private static final String PAP_SECRETS_FILE = "/etc/ppp/pap-secrets";

    private static PapLinux instance = null;

    /**
     * PapLinux constructor
     */
    private PapLinux() {
        super(PAP_SECRETS_FILE);
    }

    /**
     * Get a singleton instance
     *
     * @return PapLinux
     */
    public static PapLinux getInstance() {
        if (instance == null) {
            instance = new PapLinux();
        }

        return instance;
    }
}
