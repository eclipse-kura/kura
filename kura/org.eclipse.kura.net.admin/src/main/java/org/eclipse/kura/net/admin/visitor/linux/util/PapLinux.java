/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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

    private static PapLinux s_instance = null;

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
        if (s_instance == null) {
            s_instance = new PapLinux();
        }

        return s_instance;
    }
}
